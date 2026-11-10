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
import org.springframework.http.HttpCookie;
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
 * <p>流程（顺序不能变）：剥离入站用户头 → 优先取 Session Cookie（否则回退 Bearer）→
 * 校验 Redis 会话 → 授权判断（{@code /api/admin/**} 必须 ADMIN）→ 注入 {@code X-User-Id} / {@code X-User-Role}。
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
            "/api/admin/auth/captcha",
            "/api/admin/auth/login",
            "/api/admin/auth/register",
            "/api/admin/auth/logout",
            "/api/public/**");

    /** 管理后台路径要求的角色（授权：认证通过 ≠ 有权限） */
    private static final String ADMIN_ROLE = "ADMIN";

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

        // ② 优先 Session + Cookie：浏览器自动携带 HttpOnly Cookie，服务端查 Redis 会话
        String sessionId = cookieValue(sanitized, HeaderNames.COOKIE_SESSION);
        if (StringUtils.hasText(sessionId)) {
            return authenticateBySession(exchange, chain, sanitized, sessionId, path);
        }

        // ③ 兼容 Authorization: Bearer（开发夹具 / API 客户端）；浏览器正常流程不走这里
        return authenticateByBearer(exchange, chain, sanitized, path, traceId);
    }

    /** Session + Cookie 鉴权：sessionId 是不透明随机串，只用于查 Redis，不解析任何声明。 */
    private Mono<Void> authenticateBySession(ServerWebExchange exchange, GatewayFilterChain chain,
                                             ServerHttpRequest sanitized, String sessionId, String path) {
        return redis.opsForValue().get(CacheKeys.authSession(sessionId))
                .defaultIfEmpty("")
                .flatMap(json -> {
                    if (json.isEmpty()) {
                        // 不记录 sessionId：它本身就是凭证
                        log.info("会话不存在（已登出或已过期） path={}", path);
                        return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
                    }
                    SessionPayload payload;
                    try {
                        payload = objectMapper.readValue(json, SessionPayload.class);
                    } catch (Exception e) {
                        log.warn("会话内容解析失败 path={}", path);
                        return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
                    }
                    if (payload.userId() == null) {
                        return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
                    }
                    return forwardAsUser(exchange, chain, sanitized, payload.userId(), payload.role(), path);
                });
    }

    /** Bearer 鉴权（兼容路径）：验签 → 校验 Redis 会话 → 注入用户上下文。 */
    private Mono<Void> authenticateByBearer(ServerWebExchange exchange, GatewayFilterChain chain,
                                            ServerHttpRequest sanitized, String path, String traceId) {
        String token = stripBearer(sanitized.getHeaders().getFirst(HeaderNames.AUTHORIZATION));
        if (token == null) {
            return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtVerifier.verify(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("令牌校验失败 path={} traceId={} reason={}", path, traceId, e.getMessage());
            return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
        }

        String jti = claims.getId();
        Long userId = toLong(claims.get(ClaimNames.USER_ID));
        String role = claims.get(ClaimNames.ROLE, String.class);
        if (jti == null || userId == null) {
            return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
        }

        return redis.hasKey(CacheKeys.authSession(jti))
                .defaultIfEmpty(Boolean.FALSE)
                .flatMap(exists -> {
                    if (!Boolean.TRUE.equals(exists)) {
                        log.info("会话不存在（已登出或已过期）userId={} traceId={}", userId, traceId);
                        return respond(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
                    }
                    return forwardAsUser(exchange, chain, sanitized, userId, role, path);
                });
    }

    /**
     * 授权校验 + 注入用户上下文。
     *
     * <p>认证只回答"你是谁"；这里再回答"你能不能进"：{@code /api/admin/**} 必须是 ADMIN。
     * 其余路径暂不做细粒度 RBAC（F9.2 再按 role_code + 数据范围展开）。
     */
    private Mono<Void> forwardAsUser(ServerWebExchange exchange, GatewayFilterChain chain,
                                     ServerHttpRequest sanitized, long userId, String role, String path) {
        if (path.startsWith("/api/admin/") && !ADMIN_ROLE.equals(role)) {
            log.info("越权访问 path={} userId={} role={}", path, userId, role);
            return respond(exchange, HttpStatus.FORBIDDEN, ErrorCode.NO_PERMISSION);
        }
        ServerHttpRequest authenticated = sanitized.mutate()
                .headers(headers -> {
                    headers.set(HeaderNames.USER_ID, String.valueOf(userId));
                    headers.set(HeaderNames.USER_ROLE, role == null ? "" : role);
                })
                .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private static String cookieValue(ServerHttpRequest request, String name) {
        HttpCookie cookie = request.getCookies().getFirst(name);
        return cookie == null ? null : cookie.getValue();
    }

    /** 鉴权/授权失败也返回统一响应体（网关只引 common-core 的原因），HTTP 状态码只表达传输语义。 */
    private Mono<Void> respond(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
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

    /** Redis 会话内容（auth-service 写入）：只放 userId/role，不含任何隐私字段。 */
    private record SessionPayload(Long userId, String role) {
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
