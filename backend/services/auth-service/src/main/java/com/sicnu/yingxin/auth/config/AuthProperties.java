package com.sicnu.yingxin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 认证配置（开发规范 §4.1）：
 * 密钥由环境变量注入（{@code JWT_SECRET}，长度 ≥ 32 字节，不得写入仓库）；访问令牌有效期 2 小时。
 *
 * <p>本地默认值仅供开发：`JWT_SECRET` 未注入时用长度足够的占位串，云端必须注入真实随机值。
 */
@Component
@ConfigurationProperties(prefix = "yingxin.auth")
public class AuthProperties {

    /** HS256 密钥，长度 ≥ 32 字节 */
    private String jwtSecret = "yingxin-local-dev-jwt-secret-change-me-32bytes";

    /** 访问令牌有效期（开发规范 §4.1：2 小时） */
    private Duration accessTokenTtl = Duration.ofHours(2);

    /** 脚手架验证夹具（默认关闭；不是业务功能，见 DevTokenFixtureController） */
    private Fixture fixture = new Fixture();

    public static class Fixture {

        /** 是否暴露 /api/dev/** 令牌签发夹具；上云前必须为 false */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }
}
