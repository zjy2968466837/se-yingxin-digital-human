package com.sicnu.yingxin.auth.service.impl;

import com.sicnu.yingxin.auth.dto.LoginRequest;
import com.sicnu.yingxin.auth.entity.AdminUser;
import com.sicnu.yingxin.auth.entity.AuthUser;
import com.sicnu.yingxin.auth.error.AuthErrorCode;
import com.sicnu.yingxin.auth.mapper.AdminUserMapper;
import com.sicnu.yingxin.auth.mapper.AuthUserMapper;
import com.sicnu.yingxin.auth.service.AuthService;
import com.sicnu.yingxin.auth.service.JwtService;
import com.sicnu.yingxin.auth.service.RbacService;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.auth.vo.GroupVO;
import com.sicnu.yingxin.auth.vo.LoginVO;
import com.sicnu.yingxin.auth.vo.MeVO;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务实现（业务阶段）。
 *
 * <p>登录链路：锁定检查 → 审批状态检查 → BCrypt 密码校验 → JwtService 签发 →
 * SessionService 写 Redis 会话（TTL 与令牌一致）→ 返回令牌 + 组 + 权限点。
 * 权限相关动作写 auth_audit_log；登录失败连续 5 次临时锁定 10 分钟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final AuthUserMapper authUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final RbacService rbacService;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginRequest request, String clientIp, String userAgent) {
        Integer loginType = request.getLoginType();
        if (loginType == null) {
            loginType = 1;
        }

        if (rbacService.isLocked(request.getUsername())) {
            throw new BizException(AuthErrorCode.ACCOUNT_LOCKED, "登录失败次数过多，账号已临时锁定，请稍后再试");
        }

        Long userId;
        String role;
        String realName;
        List<GroupVO> groups;
        List<String> perms;
        try {
            switch (loginType) {
                // 学生/教职工：auth_user + 禅道组权限
                case 1 -> {
                    AuthUser user = authUserMapper.selectByStudentNo(request.getUsername());
                    if (user == null) {
                        throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
                    }
                    if (user.getApproveStatus() != null && user.getApproveStatus() == 0) {
                        throw new BizException(AuthErrorCode.APPROVE_REQUIRED, "账号正在等待管理员审批，请稍后再试");
                    }
                    if (user.getApproveStatus() != null && user.getApproveStatus() == 2) {
                        throw new BizException(AuthErrorCode.APPROVE_REQUIRED, "注册申请已被驳回，请联系管理员");
                    }
                    if (user.getStatus() != null && user.getStatus() == 2) {
                        throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                    }
                    validatePassword(request.getPassword(), user.getPasswordHash());

                    userId = user.getId();
                    groups = rbacService.groupsOf(userId);
                    perms = rbacService.permsOf(userId);
                    role = rbacService.inSuperGroup(userId) ? Roles.ADMIN : Roles.STUDENT;
                    realName = user.getRealName();

                    user.setLastLoginTime(LocalDateTime.now());
                    authUserMapper.updateById(user);
                }
                // 后台管理员：admin_user 超级账号，权限点为 ALL
                case 2 -> {
                    AdminUser admin = adminUserMapper.selectByUsername(request.getUsername());
                    if (admin == null) {
                        throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
                    }
                    if (admin.getStatus() != null && admin.getStatus() == 2) {
                        throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                    }
                    validatePassword(request.getPassword(), admin.getPasswordHash());

                    userId = admin.getId();
                    role = Roles.ADMIN;
                    realName = admin.getRealName();
                    groups = List.of(new GroupVO("admin", "系统管理员"));
                    perms = List.of("ALL");

                    admin.setLastLoginTime(LocalDateTime.now());
                    adminUserMapper.updateById(admin);
                }
                default -> throw new BizException(AuthErrorCode.LOGIN_TYPE_INVALID, "登录类型不合法");
            }
        } catch (BizException e) {
            rbacService.recordLoginFailure(request.getUsername());
            rbacService.audit(request.getUsername(), "login", request.getUsername(), "登录失败：" + e.getMessage());
            throw e;
        }

        rbacService.clearLoginFailure(request.getUsername());
        rbacService.audit(request.getUsername(), "login", request.getUsername(),
                "登录成功 role=" + role);

        JwtService.IssuedToken issued = jwtService.issue(userId, role);
        sessionService.save(issued.jti(), userId, role, jwtService.getAccessTokenTtl());

        log.info("用户登录成功 userId={} role={}", userId, role);
        LoginVO vo = new LoginVO(issued.token(), "Bearer", issued.expiresInSeconds(), userId, role);
        vo.setRealName(realName);
        vo.setGroups(groups);
        vo.setPerms(perms);
        return vo;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(AuthErrorCode.TOKEN_REQUIRED, "缺少访问令牌");
        }
        String jti;
        try {
            Claims claims = jwtService.parse(stripBearer(token)).getPayload();
            jti = claims.getId();
        } catch (Exception e) {
            // 过期或非法令牌直接视为已登出
            return;
        }
        sessionService.remove(jti);
        log.info("会话已注销 jti={}", jti);
    }

    @Override
    public MeVO me(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        MeVO vo = new MeVO();
        vo.setUserId(userId);
        String ctxRole = com.sicnu.yingxin.common.web.context.UserContextHolder.getRole();
        vo.setRole(ctxRole);
        AuthUser user = authUserMapper.selectById(userId);
        if (user != null) {
            vo.setAccount(user.getStudentNo());
            vo.setRealName(user.getRealName());
            vo.setGroups(rbacService.groupsOf(userId));
            vo.setPerms(rbacService.permsOf(userId));
            return vo;
        }
        AdminUser admin = adminUserMapper.selectById(userId);
        if (admin != null) {
            vo.setAccount(admin.getUsername());
            vo.setRealName(admin.getRealName());
            vo.setGroups(List.of(new GroupVO("admin", "系统管理员")));
            vo.setPerms(List.of("ALL"));
            return vo;
        }
        throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
    }

    private void validatePassword(String raw, String hash) {
        if (hash == null || hash.isBlank()) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(raw, hash)) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
    }

    private String stripBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring("Bearer ".length());
        }
        return token;
    }
}
