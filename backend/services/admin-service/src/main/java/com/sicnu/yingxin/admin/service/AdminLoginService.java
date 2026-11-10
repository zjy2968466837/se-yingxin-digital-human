package com.sicnu.yingxin.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sicnu.yingxin.admin.client.AuthSessionClient;
import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.constant.AdminRoles;
import com.sicnu.yingxin.admin.dto.AdminLoginRequest;
import com.sicnu.yingxin.admin.entity.AdminUser;
import com.sicnu.yingxin.admin.mapper.AdminUserMapper;
import com.sicnu.yingxin.admin.vo.AdminAuthResult;
import com.sicnu.yingxin.admin.vo.AdminLoginVO;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员登录（FR-9.1）。
 *
 * <p>校验顺序固定：**验证码 → 锁定检查 → 账号与口令 → 账号状态 → 创建会话**。
 * 三个安全约定：
 * ① 账号不存在与口令错误返回同一个 1003，避免账号枚举；
 * ② 账号不存在时也跑一次 BCrypt，抹平时序差异；
 * ③ 口令明文只在内存里参与一次比对，不落库、不进日志、不回传；会话 id 由 auth-service 生成，
 *    最终只写进 HttpOnly Cookie。
 */
@Service
public class AdminLoginService {

    /** 账号不存在时用于对齐耗时的散列（口令本身无意义） */
    private static final String DUMMY_HASH = "$2b$12$dQ3MNnDK4ecDun4Y6JBWG.4L2.AbTxpmlt4..MpNyxvzVCVJ5tItS";

    private final AdminUserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final LoginFailCounter failCounter;
    private final AuthSessionClient sessionClient;
    private final AdminAuthProperties properties;

    public AdminLoginService(AdminUserMapper mapper, PasswordEncoder passwordEncoder,
                             CaptchaService captchaService, LoginFailCounter failCounter,
                             AuthSessionClient sessionClient, AdminAuthProperties properties) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.failCounter = failCounter;
        this.sessionClient = sessionClient;
        this.properties = properties;
    }

    public AdminAuthResult login(AdminLoginRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

        String username = request.getUsername().trim();
        if (failCounter.isLocked(username)) {
            throw new BizException(AdminErrorCodes.LOGIN_LOCKED,
                    "登录失败次数过多，请 " + properties.getLoginFailWindow().toMinutes() + " 分钟后再试");
        }

        AdminUser user = mapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username)
                .eq(AdminUser::getDeleted, 0L));

        boolean matched = passwordEncoder.matches(request.getPassword(),
                user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !matched) {
            failCounter.increase(username);
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        failCounter.reset(username);
        updateLastLogin(user.getId());

        AuthSessionClient.SessionResponse session = sessionClient.create(user.getId(), AdminRoles.ADMIN);
        AdminLoginVO vo = new AdminLoginVO(user.getUsername(), user.getRealName(),
                AdminRoles.ADMIN, session.expiresIn());
        return new AdminAuthResult(vo, session.sessionId());
    }

    /** 只更新 last_login_time，其余字段交给审计填充（create_time / update_time 不动）。 */
    private void updateLastLogin(Long userId) {
        AdminUser patch = new AdminUser();
        patch.setId(userId);
        patch.setLastLoginTime(LocalDateTime.now());
        mapper.updateById(patch);
    }
}
