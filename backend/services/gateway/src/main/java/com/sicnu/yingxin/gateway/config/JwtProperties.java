package com.sicnu.yingxin.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * JWT 配置（规范 §4.1）。
 *
 * <p>网关只解析/校验令牌，不签发；密钥由 {@code JWT_SECRET} 注入。
 */
@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "yingxin.jwt")
public class JwtProperties {

    private String secret;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 缺失或长度不足 32 字节，网关无法校验令牌");
        }
        log.info("网关 JWT 配置加载完成");
    }
}
