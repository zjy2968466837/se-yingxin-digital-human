package com.sicnu.yingxin.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 会话存储（开发规范 §4.1）：Redis 保存 {@code sessionId → userId/role}，**TTL 与 Cookie 一致**；
 * 登出即删 key，会话立即失效（F1.8 验收准则）。
 *
 * <p>Session + Cookie 方案下 sessionId 是**不透明随机串**（32 字节 CSPRNG，64 位十六进制），
 * 与用户信息无关，不承载任何声明；网关只做 key 查询，不解析内容。
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final int SESSION_ID_BYTES = 32;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public SessionService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 新建会话并返回不透明 sessionId（写 Cookie 用）。 */
    public String create(long userId, String role, Duration ttl) {
        String sessionId = newSessionId();
        save(sessionId, userId, role, ttl);
        return sessionId;
    }

    public void save(String sessionId, long userId, String role, Duration ttl) {
        try {
            String value = objectMapper.writeValueAsString(new SessionPayload(userId, role));
            redis.opsForValue().set(CacheKeys.authSession(sessionId), value, ttl);
        } catch (JsonProcessingException e) {
            // 会话写入失败必须抛出：否则下发了 Cookie 却没有会话，网关会一律判为无效
            throw new IllegalStateException("会话写入失败", e);
        }
    }

    /** 读取会话；不存在或内容损坏返回 null（网关按"未登录"处理）。 */
    public SessionPayload find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        String value = redis.opsForValue().get(CacheKeys.authSession(sessionId));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, SessionPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("会话内容解析失败，按无效会话处理（可能被写坏）");
            return null;
        }
    }

    public void remove(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            redis.delete(CacheKeys.authSession(sessionId));
        }
    }

    private String newSessionId() {
        byte[] bytes = new byte[SESSION_ID_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record SessionPayload(long userId, String role) {
    }
}
