package com.sicnu.yingxin.auth.service;

import com.sicnu.yingxin.auth.config.AuthProperties;
import com.sicnu.yingxin.common.core.constant.ClaimNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析（开发规范 §4.1）：HS256，负载只放 {@code userId / role / exp}，
 * **不放姓名、学号、身份证等隐私字段**；仅 auth-service 签发。
 */
@Service
public class JwtService {

    /** 负载中的声明名（与网关共享，见 common-core 的 {@code ClaimNames}） */
    public static final String CLAIM_USER_ID = ClaimNames.USER_ID;
    public static final String CLAIM_ROLE = ClaimNames.ROLE;

    private final SecretKey key;
    private final AuthProperties properties;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度必须 ≥ 32 字节（当前 " + secret.length + "）");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    /** 签发访问令牌：返回令牌本体、jti（用于会话键）与有效期秒数。 */
    public IssuedToken issue(long userId, String role) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        String token = Jwts.builder()
                .id(jti)                                  // jti → Redis 会话键
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, jti, properties.getAccessTokenTtl().toSeconds());
    }

    /** 解析并校验签名与有效期；失败抛 {@code JwtException}，由调用方决定错误码。 */
    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    /** 会话键 TTL 与令牌一致（开发规范 §4.1），供会话存储复用。 */
    public java.time.Duration getAccessTokenTtl() {
        return properties.getAccessTokenTtl();
    }

    public record IssuedToken(String token, String jti, long expiresInSeconds) {
    }
}
