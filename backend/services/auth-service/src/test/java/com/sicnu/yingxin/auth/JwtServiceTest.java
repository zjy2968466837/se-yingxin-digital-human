package com.sicnu.yingxin.auth;

import com.sicnu.yingxin.auth.config.AuthProperties;
import com.sicnu.yingxin.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 令牌签发与解析（开发规范 §4.1）：负载只放 userId / role / exp / jti，2 小时有效期，密钥不足 32 字节直接拒绝启动。
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789-abcdef";

    private AuthProperties properties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.setJwtSecret(SECRET);
        properties.setAccessTokenTtl(Duration.ofHours(2));
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("签发的令牌可被解析，负载含 userId / role / jti，有效期 7200 秒")
    void issueThenParse() {
        JwtService.IssuedToken issued = jwtService.issue(1000000000000000001L, "STUDENT");

        assertThat(issued.expiresInSeconds()).isEqualTo(7200);
        assertThat(issued.jti()).hasSize(32);

        Claims claims = jwtService.parse(issued.token()).getPayload();
        assertThat(claims.getId()).isEqualTo(issued.jti());
        assertThat(((Number) claims.get(JwtService.CLAIM_USER_ID)).longValue()).isEqualTo(1000000000000000001L);
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("STUDENT");
        // 隐私字段不得进负载
        assertThat(claims.toString()).doesNotContain("studentNo", "idCard", "phone");
    }

    @Test
    @DisplayName("被篡改的令牌解析失败")
    void tamperedTokenRejected() {
        String token = jwtService.issue(1L, "STUDENT").token();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "bb" : "aa");

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期令牌解析失败（ExpiredJwtException）")
    void expiredTokenRejected() {
        properties.setAccessTokenTtl(Duration.ofSeconds(-60));
        JwtService shortLived = new JwtService(properties);
        String token = shortLived.issue(1L, "STUDENT").token();

        assertThatThrownBy(() -> shortLived.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("密钥不足 32 字节时拒绝启动，避免弱密钥上线")
    void weakSecretRejected() {
        AuthProperties weak = new AuthProperties();
        weak.setJwtSecret("too-short");

        assertThatThrownBy(() -> new JwtService(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
