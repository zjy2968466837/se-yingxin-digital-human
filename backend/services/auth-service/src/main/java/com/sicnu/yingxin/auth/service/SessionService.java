package com.sicnu.yingxin.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 会话存储（开发规范 §4.1）：Redis 保存 {@code jti → userId/role}，**TTL 与令牌一致**；
 * 登出即删 key，令牌立即失效（F1.8 验收准则）。
 *
 * <p>网关只校验 key 是否存在（不解析内容），因此这里存 JSON 文本即可。
 */
@Service
public class SessionService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SessionService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(String jti, long userId, String role, Duration ttl) {
        try {
            String value = objectMapper.writeValueAsString(new SessionPayload(userId, role));
            redis.opsForValue().set(CacheKeys.authSession(jti), value, ttl);
        } catch (JsonProcessingException e) {
            // 会话写入失败必须抛出：否则签发了令牌却没有会话，网关会一律判为无效
            throw new IllegalStateException("会话写入失败", e);
        }
    }

    public void remove(String jti) {
        redis.delete(CacheKeys.authSession(jti));
    }

    public record SessionPayload(long userId, String role) {
    }
}
