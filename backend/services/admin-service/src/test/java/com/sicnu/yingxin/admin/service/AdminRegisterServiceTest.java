package com.sicnu.yingxin.admin.service;

import com.sicnu.yingxin.admin.client.AuthSessionClient;
import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.dto.AdminRegisterRequest;
import com.sicnu.yingxin.admin.entity.AdminUser;
import com.sicnu.yingxin.admin.mapper.AdminUserMapper;
import com.sicnu.yingxin.admin.vo.AdminAuthResult;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自助注册单元测试：开关、验证码、两次口令、用户名唯一、会话创建。
 */
@ExtendWith(MockitoExtension.class)
class AdminRegisterServiceTest {

    @Mock
    AdminUserMapper mapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    CaptchaService captchaService;

    @Mock
    AuthSessionClient sessionClient;

    private final AdminAuthProperties properties = new AdminAuthProperties();

    private AdminRegisterService service;

    @BeforeEach
    void setUp() {
        service = new AdminRegisterService(mapper, passwordEncoder, captchaService, sessionClient, properties);
    }

    private AdminRegisterRequest request() {
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setUsername("new_admin");
        request.setRealName("新管理员");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setCaptchaId("captcha-1");
        request.setCaptchaCode("AB34");
        return request;
    }

    @Test
    @DisplayName("注册成功：BCrypt 入库 + 直接创建会话")
    void register_success() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Password123")).thenReturn("$2b$12$new-hash");
        doAnswer(inv -> {
            AdminUser saved = inv.getArgument(0);
            saved.setId(9001L);
            return 1;
        }).when(mapper).insert(any(AdminUser.class));
        when(sessionClient.create(9001L, "ADMIN"))
                .thenReturn(new AuthSessionClient.SessionResponse("sid-new", 7200));

        AdminAuthResult result = service.register(request());

        assertThat(result.sessionId()).isEqualTo("sid-new");
        assertThat(result.user().username()).isEqualTo("new_admin");
        assertThat(result.user().role()).isEqualTo("ADMIN");
        verify(mapper).insert(any(AdminUser.class));
        verify(sessionClient).create(eq(9001L), eq("ADMIN"));
    }

    @Test
    @DisplayName("用户名已存在：2804，不写库、不建会话")
    void register_duplicateUsername() {
        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.USERNAME_EXISTS);
        verify(mapper, never()).insert(any(AdminUser.class));
        verify(sessionClient, never()).create(anyLong(), anyString());
    }

    @Test
    @DisplayName("注册关闭：2805，且先于验证码校验")
    void register_disabled() {
        properties.setRegisterEnabled(false);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.REGISTER_DISABLED);
        verify(captchaService, never()).verify(anyString(), anyString());
        verify(mapper, never()).insert(any(AdminUser.class));
    }

    @Test
    @DisplayName("两次口令不一致：3002，不写库")
    void register_passwordMismatch() {
        AdminRegisterRequest request = request();
        request.setConfirmPassword("Other123456");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAM_FORMAT.getCode());
        verify(mapper, never()).insert(any(AdminUser.class));
    }

    @Test
    @DisplayName("验证码错误：2802，不写库")
    void register_badCaptcha() {
        org.mockito.Mockito.doThrow(new BizException(AdminErrorCodes.CAPTCHA_INVALID, "验证码错误"))
                .when(captchaService).verify(anyString(), anyString());

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.CAPTCHA_INVALID);
        verify(mapper, never()).insert(any(AdminUser.class));
    }
}
