package com.sicnu.yingxin.admin.service;

import com.sicnu.yingxin.admin.client.AuthSessionClient;
import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.admin.constant.AdminErrorCodes;
import com.sicnu.yingxin.admin.dto.AdminLoginRequest;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录核心逻辑的纯单元测试：不连 MySQL / Redis。
 *
 * <p>重点验证：账号不存在与口令错误同码、锁定先于查库、失败时不创建会话。
 */
@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    @Mock
    AdminUserMapper mapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    CaptchaService captchaService;

    @Mock
    LoginFailCounter failCounter;

    @Mock
    AuthSessionClient sessionClient;

    private final AdminAuthProperties properties = new AdminAuthProperties();

    private AdminLoginService service;

    @BeforeEach
    void setUp() {
        service = new AdminLoginService(mapper, passwordEncoder, captchaService, failCounter,
                sessionClient, properties);
    }

    private AdminLoginRequest request() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("demo-pass");
        request.setCaptchaId("captcha-1");
        request.setCaptchaCode("AB34");
        return request;
    }

    private AdminUser user(int status) {
        AdminUser user = new AdminUser();
        user.setId(1001L);
        user.setUsername("admin");
        user.setPasswordHash("$2b$12$hash");
        user.setRealName("演示管理员");
        user.setStatus(status);
        return user;
    }

    @Test
    @DisplayName("正确口令：创建会话并重置失败计数")
    void login_success() {
        when(mapper.selectOne(any())).thenReturn(user(1));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(sessionClient.create(1001L, "ADMIN"))
                .thenReturn(new AuthSessionClient.SessionResponse("sid-1", 7200));

        AdminAuthResult result = service.login(request());

        assertThat(result.user().username()).isEqualTo("admin");
        assertThat(result.user().realName()).isEqualTo("演示管理员");
        assertThat(result.user().role()).isEqualTo("ADMIN");
        assertThat(result.user().expiresIn()).isEqualTo(7200);
        assertThat(result.sessionId()).isEqualTo("sid-1");
        verify(failCounter).reset("admin");
        verify(mapper).updateById(any(AdminUser.class));
    }

    @Test
    @DisplayName("账号不存在：与口令错误同码 1003，且仍跑一次 BCrypt 抹平时序")
    void login_unknownUser_isBadCredentials() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.BAD_CREDENTIALS.getCode());
        verify(passwordEncoder).matches(eq("demo-pass"), anyString());
        verify(failCounter).increase("admin");
        verify(sessionClient, never()).create(anyLong(), anyString());
    }

    @Test
    @DisplayName("口令错误：1003 + 失败计数 +1")
    void login_wrongPassword_isBadCredentials() {
        when(mapper.selectOne(any())).thenReturn(user(1));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.BAD_CREDENTIALS.getCode());
        verify(failCounter).increase("admin");
        verify(mapper, never()).updateById(any(AdminUser.class));
        verify(sessionClient, never()).create(anyLong(), anyString());
    }

    @Test
    @DisplayName("账号禁用：口令正确也拒绝，返回 1004")
    void login_disabledAccount() {
        when(mapper.selectOne(any())).thenReturn(user(2));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode());
        verify(sessionClient, never()).create(anyLong(), anyString());
    }

    @Test
    @DisplayName("已锁定：不查库、不比对口令，直接 2803")
    void login_locked_shortCircuits() {
        when(failCounter.isLocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.LOGIN_LOCKED);
        verify(mapper, never()).selectOne(any());
        verify(sessionClient, never()).create(anyLong(), anyString());
    }

    @Test
    @DisplayName("验证码错误：不进入账号校验")
    void login_badCaptcha_stopsEarly() {
        doThrow(new BizException(AdminErrorCodes.CAPTCHA_INVALID, "验证码错误"))
                .when(captchaService).verify(anyString(), anyString());

        assertThatThrownBy(() -> service.login(request()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(AdminErrorCodes.CAPTCHA_INVALID);
        verify(mapper, never()).selectOne(any());
    }
}
