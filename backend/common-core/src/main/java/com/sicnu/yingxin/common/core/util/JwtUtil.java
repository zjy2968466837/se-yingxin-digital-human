package com.sicnu.yingxin.common.core.util;

import com.sicnu.yingxin.common.core.constant.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具（无 Spring 依赖）。
 *
 * <p>本类仅负责令牌的签发与解析，会话是否有效（Redis 中是否存在）由调用方另行校验。
 */
public class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 生成访问令牌。
     *
     * @param secret  密钥，长度必须 ≥ 32 字节
     * @param userId  用户 ID
     * @param role    角色
     * @param ttlHours 有效期（小时）
     * @return 生成的 JWT
     */
    public static String generateToken(String secret, Long userId, String role, int ttlHours) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString().replace("-", "");
        return Jwts.builder()
                .claim(AuthConstants.CLAIM_USER_ID, userId)
                .claim(AuthConstants.CLAIM_ROLE, role)
                .claim(AuthConstants.CLAIM_JTI, jti)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌。
     *
     * @param secret 密钥
     * @param token  令牌（含或不含 Bearer 前缀均可）
     * @return 负载声明
     * @throws JwtException 解析失败或令牌已过期
     */
    public static Claims parseToken(String secret, String token) {
        String raw = stripBearer(token);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(raw)
                .getPayload();
    }

    /** 获取令牌唯一标识 jti */
    public static String getJti(Claims claims) {
        return claims.getId();
    }

    /** 获取用户 ID */
    public static Long getUserId(Claims claims) {
        Object value = claims.get(AuthConstants.CLAIM_USER_ID);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    /** 获取角色 */
    public static String getRole(Claims claims) {
        Object value = claims.get(AuthConstants.CLAIM_ROLE);
        return value == null ? null : value.toString();
    }

    /** 判断是否为过期异常 */
    public static boolean isExpiredException(Throwable e) {
        return e instanceof ExpiredJwtException;
    }

    private static String stripBearer(String token) {
        if (token != null && token.startsWith(AuthConstants.BEARER_PREFIX)) {
            return token.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return token;
    }
}
