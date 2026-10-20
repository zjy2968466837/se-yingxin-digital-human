package com.sicnu.yingxin.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.AuthConstants;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.common.core.util.JwtUtil;
import com.sicnu.yingxin.common.core.util.SnowflakeIdUtil;
import com.sicnu.yingxin.gateway.config.AuthProperties;
import com.sicnu.yingxin.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 网关 JWT 鉴权过滤器（规范 §4.2、§4.3）。
 *
 * <p>流程：剥离入站用户头 → 取 Bearer 令牌 → 校验签名与有效期 → 校验 Redis 会话 →
 * 注入 X-User-Id/X-User-Role/X-Trace-Id → 转发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        // 在路由转发之前执行
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getSecret(), token);
        } catch (JwtException e) {
            log.warn("令牌解析失败 path={} reason={}", path, e.getMessage());
            return unauthorized(exchange, JwtUtil.isExpiredException(e) ? ErrorCode.TOKEN_INVALID : ErrorCode.TOKEN_INVALID);
        }

        String jti = JwtUtil.getJti(claims);
        Long userId = JwtUtil.getUserId(claims);
        String role = JwtUtil.getRole(claims);

        if (userId == null || !StringUtils.hasText(jti)) {
            return unauthorized(exchange, ErrorCode.TOKEN_INVALID);
        }

        String sessionKey = "yx:auth:session:" + jti;
        return redisTemplate.opsForValue()
                .get(sessionKey)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Redis 会话不存在 jti={}", jti);
                    return Mono.empty();
                }))
                .flatMap(session -> {
                    // 会话存在，注入用户上下文并转发
                    String traceId = generateTraceId(request);
                    ServerHttpRequest mutated = request.mutate()
                            .header(AuthConstants.HEADER_USER_ID, String.valueOf(userId))
                            .header(AuthConstants.HEADER_USER_ROLE, role == null ? "" : role)
                            .header(AuthConstants.HEADER_TRACE_ID, traceId)
                            .build();
                    // 刷新会话 TTL（滑动过期）
                    redisTemplate.expire(sessionKey, Duration.ofHours(2)).subscribe();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(unauthorized(exchange, ErrorCode.UNAUTHORIZED));
    }

    private boolean isWhitelisted(String path) {
        if (CollectionUtils.isEmpty(authProperties.getWhitelist())) {
            return false;
        }
        return authProperties.getWhitelist().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AuthConstants.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        if (header.startsWith(AuthConstants.BEARER_PREFIX)) {
            return header.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return header;
    }

    private String generateTraceId(ServerHttpRequest request) {
        // 优先复用入站 traceId（内部服务间调用），否则生成新的
        String incoming = request.getHeaders().getFirst(AuthConstants.HEADER_TRACE_ID);
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return Long.toHexString(SnowflakeIdUtil.INSTANCE.nextId());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> body = Result.failure(errorCode);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":1001,\"message\":\"未登录\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
