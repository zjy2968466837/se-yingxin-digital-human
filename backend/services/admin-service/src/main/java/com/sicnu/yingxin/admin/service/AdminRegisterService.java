package com.sicnu.yingxin.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sicnu.yingxin.admin.client.AuthSessionClient;
import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.constant.AdminRoles;
import com.sicnu.yingxin.admin.dto.AdminRegisterRequest;
import com.sicnu.yingxin.admin.entity.AdminUser;
import com.sicnu.yingxin.admin.mapper.AdminUserMapper;
import com.sicnu.yingxin.admin.vo.AdminAuthResult;
import com.sicnu.yingxin.admin.vo.AdminLoginVO;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员自助注册（FR-9.1 补充）。
 *
 * <p>顺序：**开关 → 验证码 → 两次口令一致 → 用户名唯一 → BCrypt 入库 → 建会话**。
 * 注册即登录，返回的会话 id 由 Controller 写入 HttpOnly Cookie。
 *
 * <p>生产纪律：自助注册默认只适合课程演示，云端必须 `AUTH_REGISTER_ENABLED=false`，
 * 账号由信息中心/超管创建。
 */
@Service
public class AdminRegisterService {

    private final AdminUserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final AuthSessionClient sessionClient;
    private final AdminAuthProperties properties;

    public AdminRegisterService(AdminUserMapper mapper, PasswordEncoder passwordEncoder,
                                CaptchaService captchaService, AuthSessionClient sessionClient,
                                AdminAuthProperties properties) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.sessionClient = sessionClient;
        this.properties = properties;
    }

    public AdminAuthResult register(AdminRegisterRequest request) {
        if (!properties.isRegisterEnabled()) {
            throw new BizException(AdminErrorCodes.REGISTER_DISABLED, "注册入口未开放，请联系管理员");
        }
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_FORMAT, "两次输入的密码不一致");
        }

        String username = request.getUsername().trim();
        Long exists = mapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username)
                .eq(AdminUser::getDeleted, 0L));
        if (exists != null && exists > 0) {
            throw new BizException(AdminErrorCodes.USERNAME_EXISTS, "用户名已被占用，请换一个");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setDept(5);      // 5 信息中心（ACT-4），注册账号默认归属
        user.setStatus(1);
        try {
            mapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同一用户名：唯一索引兜底，转成同码提示
            throw new BizException(AdminErrorCodes.USERNAME_EXISTS, "用户名已被占用，请换一个");
        }

        AuthSessionClient.SessionResponse session = sessionClient.create(user.getId(), AdminRoles.ADMIN);
        AdminLoginVO vo = new AdminLoginVO(user.getUsername(), user.getRealName(),
                AdminRoles.ADMIN, session.expiresIn());
        return new AdminAuthResult(vo, session.sessionId());
    }
}
