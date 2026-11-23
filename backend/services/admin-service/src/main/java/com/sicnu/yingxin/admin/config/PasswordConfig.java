package com.sicnu.yingxin.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 口令散列配置（开发规范 §5.5）：BCrypt 慢哈希，盐由算法内置。
 *
 * <p>cost 越高越抗爆破，但登录越慢；12 在演示机上约 0.2–0.3 秒，与"连续失败 5 次锁定"配合足够。
 * 范围卡死在 10–14，避免把配置写坏后要么形同裸哈希、要么把登录拖垮。
 */
@Configuration(proxyBeanMethods = false)
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder(AdminAuthProperties properties) {
        int cost = properties.getBcryptCost();
        if (cost < 10 || cost > 14) {
            throw new IllegalStateException("yingxin.auth.bcrypt-cost 必须在 10–14 之间（当前 " + cost + "）");
        }
        return new BCryptPasswordEncoder(cost);
    }
}
