package com.sicnu.yingxin.qa.cache;

import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.qa.vo.QaSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 会话读缓存（S5 样板）：key 走 {@link CacheKeys} 统一口径，值 JSON 序列化，**必须设 TTL**。
 *
 * <p>纪律（开发规范 §5.4）：缓存只做加速，不得作为唯一数据源；Redis 不可用时降级为直接查库，
 * 不能让缓存故障变成业务故障。删除/更新由写入方主动失效。
 */
@Slf4j
@Component
public class QaSessionCache {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public QaSessionCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 未命中或 Redis 异常返回 null，调用方回源数据库。 */
    public QaSessionVO get(Long id) {
        try {
            Object value = redisTemplate.opsForValue().get(CacheKeys.qaSession(id));
            return value instanceof QaSessionVO vo ? vo : null;
        } catch (RuntimeException e) {
            log.warn("读取会话缓存失败，回源数据库 id={} reason={}", id, e.getMessage());
            return null;
        }
    }

    public void put(QaSessionVO vo) {
        try {
            redisTemplate.opsForValue().set(CacheKeys.qaSession(vo.getId()), vo, TTL);
        } catch (RuntimeException e) {
            log.warn("写入会话缓存失败 id={} reason={}", vo.getId(), e.getMessage());
        }
    }

    public void evict(Long id) {
        try {
            redisTemplate.delete(CacheKeys.qaSession(id));
        } catch (RuntimeException e) {
            log.warn("删除会话缓存失败 id={} reason={}", id, e.getMessage());
        }
    }
}
