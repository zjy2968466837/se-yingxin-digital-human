package com.sicnu.yingxin.admin.service;

import com.sicnu.yingxin.admin.config.AdminAuthProperties;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录失败计数（NFR-4.5 防撞库）：按用户名固定窗口计数，达到阈值后在窗口内拒绝登录。
 *
 * <p>计数放 Redis 而不是内存，服务多实例时同样生效；窗口过期即自动解锁。
 * 成功登录清零，避免"历史失败"拖累正常用户。
 */
@Component
public class LoginFailCounter {

    private final StringRedisTemplate redis;
    private final AdminAuthProperties properties;

    public LoginFailCounter(StringRedisTemplate redis, AdminAuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 是否已达锁定阈值。 */
    public boolean isLocked(String username) {
        String value = redis.opsForValue().get(CacheKeys.adminLoginFail(username));
        if (value == null) {
            return false;
        }
        try {
            return Long.parseLong(value) >= properties.getLoginFailLockTimes();
        } catch (NumberFormatException e) {
            // 值被写坏时按未锁定处理，并清掉脏数据（否则该用户名会永久无法登录）
            redis.delete(CacheKeys.adminLoginFail(username));
            return false;
        }
    }

    /** 失败次数 +1；首次计数时设置窗口 TTL。 */
    public long increase(String username) {
        String key = CacheKeys.adminLoginFail(username);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, properties.getLoginFailWindow());
        }
        return count == null ? 0 : count;
    }

    public void reset(String username) {
        redis.delete(CacheKeys.adminLoginFail(username));
    }
}
