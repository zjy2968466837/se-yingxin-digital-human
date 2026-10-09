package com.sicnu.yingxin.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.constant.ClaimNames;
import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * 网关统一鉴权（H-04 / H-13，开发规范 §4.2）——**唯一的鉴权入口**。
 *
 * <p>流程（顺序不能变）：剥离入站用户头 → 取 {@code Authorization: Bearer} →
 * 校验签名与有效期 → 校验 Redis 会话存在 → 注入 {@code X-User-Id} / {@code X-User-Role}。
 *
 * <p>三个安全前提：
 * ① 入站同名头**先删再注入**，否则外部可伪造身份（伪造 {@code X-User-Id} 不得放行）；
 * ② 白名单只放行「自检 / 登录 / 公开内容」，{@code /actuator/**}、{@code /swagger-ui.html} 默认不放行；
 * ③ 登出即删会话键，旧令牌立即失效（F1.8）——因此每次请求都要查 Redis。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 白名单（开发规范 §4.2 表格，只有这些路径免鉴权） */
    private static final List<String> WHITELIST = List.of(
            "/api/*/ping",
            "/api/auth/login/**",
            "/api/auth/wechat/**",
            "/api/public/**");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AuthProperties properties;
    private final JwtVerifier jwtVerifier;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AuthGlobalFilter(AuthProperties properties, JwtVerifier jwtVerifier,
                            ReactiveStringRedisTemplate redis, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jwtVerifier = jwtVerifier;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String traceId = resolveTraceId(request);

        // ① 剥离入站用户头（防伪造）+ 注入 traceId；无论是否白名单都要做
        ServerHttpRequest sanitized = request.mutate()
                .headers(headers -> {
                    headers.remove(HeaderNames.USER_ID);
                    headers.remove(HeaderNames.USER_ROLE);
                    headers.set(HeaderNames.TRACE_ID, traceId);
                })
                .build();
        exchange.getResponse().getHeaders().set(HeaderNames.TRACE_ID, traceId);

        if (!properties.isEnabled() || isWhitelisted(path)) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String token = stripBearer(sanitized.getHeaders().getFirst(HeaderNames.AUTHORIZATION));
        if (token == null) {
            return unauthorized(exchange, ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtVerifier.verify(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("令牌校验失败 path={} traceId={} reason={}", path, traceId, e.getMessage());
            return unauthorized(exchange, ErrorCode.TOKEN_INVALID);
        }

        String jti = claims.getId();
        Long userId = toLong(claims.get(ClaimNames.USER_ID));
        String role = claims.get(ClaimNames.ROLE, String.class);
        if (jti == null || userId == null) {
            return unauthorized(exchange, ErrorCode.TOKEN_INVALID);
        }

        // ② 会话必须存在：登出/续期控制都在这里生效
        return redis.hasKey(CacheKeys.authSession(jti))
                .defaultIfEmpty(Boolean.FALSE)
                .flatMap(exists -> {
                    if (!Boolean.TRUE.equals(exists)) {
                        log.info("会话不存在（已登出或已过期）userId={} jti={} traceId={}", userId, jti, traceId);
                        return unauthorized(exchange, ErrorCode.TOKEN_INVALID);
                    }
                    ServerHttpRequest authenticated = sanitized.mutate()
                            .headers(headers -> {
                                headers.set(HeaderNames.USER_ID, String.valueOf(userId));
                                headers.set(HeaderNames.USER_ROLE, role == null ? "" : role);
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(authenticated).build());
                });
    }

    /** 鉴权失败也返回统一响应体（网关只引 common-core 的原因），HTTP 401 只表达传输语义。 */
    private Mono<Void> unauthorized(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Result.fail(errorCode));
        } catch (Exception e) {
            body = ("{\"code\":" + errorCode.getCode() + ",\"message\":\"" + errorCode.getMessage()
                    + "\",\"data\":null}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        for (String pattern : properties.getExtraWhitelist()) {
            if (StringUtils.hasText(pattern) && pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(HeaderNames.TRACE_ID);
        return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString().replace("-", "");
    }

    private static String stripBearer(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(HeaderNames.BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(HeaderNames.BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static Long toLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        // 早于路由与转发执行：鉴权不通过就不该把请求转发给下游
        return -100;
    }
}
