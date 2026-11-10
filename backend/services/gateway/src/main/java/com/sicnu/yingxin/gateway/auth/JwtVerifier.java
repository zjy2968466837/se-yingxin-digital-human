package com.sicnu.yingxin.gateway.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 令牌校验（开发规范 §4.2）：校验签名与有效期。**不解析业务语义**，
 * 校验通过后由 {@link AuthGlobalFilter} 注入 {@code X-User-*} 头。
 *
 * <p>密钥与 auth-service 共用（{@code JWT_SECRET}）；这里只验证，不签发。
 */
@Component
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(AuthProperties properties) {
        byte[] secret = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度必须 ≥ 32 字节（当前 " + secret.length + "）");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    /**
     * 校验并返回负载；签名错误、过期、格式非法都抛 {@code JwtException}，
     * 由调用方统一转成 {@code 1002 令牌无效或过期}。
     */
    public Claims verify(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
