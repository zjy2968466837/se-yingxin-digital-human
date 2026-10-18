package com.sicnu.yingxin.auth.config;

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
 * <p>密钥通过环境变量 {@code JWT_SECRET} 注入，禁止写入仓库；长度必须 ≥ 32 字节。
 */
@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "yingxin.jwt")
public class JwtProperties {

    /** 访问令牌有效期，默认 2 小时 */
    private int accessTokenTtlHours = 2;

    /** 密钥，由 {@code JWT_SECRET} 注入 */
    private String secret;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 缺失或长度不足 32 字节，无法签发令牌");
        }
        log.info("JWT 配置加载完成，accessTokenTtlHours={}", accessTokenTtlHours);
    }
}
