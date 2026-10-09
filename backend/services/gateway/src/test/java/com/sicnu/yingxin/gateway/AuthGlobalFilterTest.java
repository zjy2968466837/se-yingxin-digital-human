package com.sicnu.yingxin.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.ClaimNames;
import com.sicnu.yingxin.common.core.constant.HeaderNames;
import com.sicnu.yingxin.gateway.auth.AuthGlobalFilter;
import com.sicnu.yingxin.gateway.auth.AuthProperties;
import com.sicnu.yingxin.gateway.auth.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 网关鉴权行为（开发规范 §4.2/§4.3、需求分析文档 §2.6.1 判定 6）：
 * 无令牌 401 统一体、白名单放行、有效令牌注入用户头、登出后令牌失效、伪造头不被信任。
 */
class AuthGlobalFilterTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789-abcdef";

    private AuthProperties properties;
    private JwtVerifier verifier;
    private ReactiveStringRedisTemplate redis;
    private AuthGlobalFilter filter;

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.setJwtSecret(SECRET);
        verifier = new JwtVerifier(properties);
        redis = Mockito.mock(ReactiveStringRedisTemplate.class);
        filter = new AuthGlobalFilter(properties, verifier, redis, new ObjectMapper());
        forwarded.set(null);
    }

    @Test
    @DisplayName("非白名单且无令牌：401 + 统一响应体 code 1001")
    void unauthorizedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/anything").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(exchange)).contains("\"code\":1001").contains("\"data\":null");
        assertThat(forwarded.get()).isNull();
    }

    @Test
    @DisplayName("伪造 X-User-Id 也不能免鉴权（入站同名头先剥离）")
    void forgedHeaderIsNotTrusted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/anything")
                        .header(HeaderNames.USER_ID, "999")
                        .header(HeaderNames.USER_ROLE, "ADMIN")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    @DisplayName("白名单路径免鉴权，但仍剥离伪造头并注入 traceId")
    void whitelistPassesAndSanitizes() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/ping")
                        .header(HeaderNames.USER_ID, "999")
                        .build());

        filter.filter(exchange, chain).block();

        ServerHttpRequest request = forwarded.get().getRequest();
        assertThat(request.getHeaders().getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(request.getHeaders().getFirst(HeaderNames.TRACE_ID)).hasSize(32);
    }

    @Test
    @DisplayName("有效令牌 + 会话存在：放行并注入 X-User-Id / X-User-Role")
    void validTokenInjectsUserHeaders() {
        when(redis.hasKey(anyString())).thenReturn(Mono.just(Boolean.TRUE));
        String token = token("jti-abc", 12345L, "STUDENT", 120);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/anything")
                        .header(HeaderNames.AUTHORIZATION, HeaderNames.BEARER_PREFIX + token)
                        .header(HeaderNames.TRACE_ID, "trace-from-client")
                        .build());

        filter.filter(exchange, chain).block();

        ServerHttpRequest request = forwarded.get().getRequest();
        assertThat(request.getHeaders().getFirst(HeaderNames.USER_ID)).isEqualTo("12345");
        assertThat(request.getHeaders().getFirst(HeaderNames.USER_ROLE)).isEqualTo("STUDENT");
        assertThat(request.getHeaders().getFirst(HeaderNames.TRACE_ID)).isEqualTo("trace-from-client");
    }

    @Test
    @DisplayName("令牌有效但会话已删（已登出）：401 + code 1002，旧令牌立即失效（F1.8）")
    void revokedSessionRejected() {
        when(redis.hasKey(anyString())).thenReturn(Mono.just(Boolean.FALSE));
        String token = token("jti-logged-out", 1L, "STUDENT", 120);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/anything")
                        .header(HeaderNames.AUTHORIZATION, HeaderNames.BEARER_PREFIX + token)
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(exchange)).contains("\"code\":1002");
        assertThat(forwarded.get()).isNull();
    }

    @Test
    @DisplayName("签名不符的令牌：401 + code 1002")
    void invalidSignatureRejected() {
        String foreignToken = Jwts.builder()
                .id("jti-x")
                .claim(ClaimNames.USER_ID, 1L)
                .claim(ClaimNames.ROLE, "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor("another-secret-0123456789-abcdefghijkl".getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256)
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/qa/anything")
                        .header(HeaderNames.AUTHORIZATION, HeaderNames.BEARER_PREFIX + foreignToken)
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(exchange)).contains("\"code\":1002");
    }

    private String token(String jti, long userId, String role, long ttlSeconds) {
        return Jwts.builder()
                .id(jti)
                .claim(ClaimNames.USER_ID, userId)
                .claim(ClaimNames.ROLE, role)
                .expiration(Date.from(Instant.now().plusSeconds(ttlSeconds)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private static String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBody()
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .collectList()
                .map(list -> String.join("", list))
                .block();
    }
}
