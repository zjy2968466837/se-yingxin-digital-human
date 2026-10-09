package com.sicnu.yingxin.auth;

import com.sicnu.yingxin.auth.config.AuthProperties;
import com.sicnu.yingxin.auth.fixture.DevTokenFixtureController;
import com.sicnu.yingxin.auth.service.JwtService;
import com.sicnu.yingxin.auth.service.SessionService;
import com.sicnu.yingxin.common.core.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * 夹具行为（脚手架验证手段，不是业务功能）：签发时写会话、撤销时删会话。
 *
 * <p>夹具默认关闭由 {@code @ConditionalOnProperty} 保证（本测试直接构造控制器，不启 Spring 上下文）。
 */
class DevTokenFixtureTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789-abcdef";

    private JwtService jwtService;
    private SessionService sessionService;
    private DevTokenFixtureController controller;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret(SECRET);
        jwtService = new JwtService(properties);
        sessionService = Mockito.mock(SessionService.class);
        controller = new DevTokenFixtureController(jwtService, sessionService);
    }

    @Test
    @DisplayName("签发：返回 token/jti，并写入 Redis 会话（TTL 与令牌一致）")
    void issueWritesSession() {
        Result<Map<String, Object>> result = controller.issue(42L, "STUDENT");

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = result.getData();
        assertThat(data.get("token")).isNotNull();
        assertThat(data.get("jti")).isNotNull();
        assertThat(data.get("expiresIn")).isEqualTo(7200L);

        ArgumentCaptor<String> jti = ArgumentCaptor.forClass(String.class);
        verify(sessionService).save(jti.capture(), eq(42L), eq("STUDENT"), any());
        assertThat(jti.getValue()).isEqualTo(data.get("jti"));
        // 令牌可被解析（与网关用同一密钥校验）
        assertThat(jwtService.parse((String) data.get("token")).getPayload().getId()).isEqualTo(jti.getValue());
    }

    @Test
    @DisplayName("撤销：删除会话键（等价登出，令牌立即失效）")
    void revokeRemovesSession() {
        controller.revoke("jti-to-revoke");

        verify(sessionService).remove(eq("jti-to-revoke"));
        Mockito.verify(sessionService, Mockito.never()).save(anyString(), anyLong(), anyString(), any());
    }
}
