package com.sicnu.yingxin.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置（开发规范 §4.2）。
 *
 * <p>{@code extraWhitelist} 的用途写清楚：`/actuator/**`、`/swagger-ui.html` **默认不放行**，
 * 本地调试可临时加进来，云端必须保持为空（F11.7）。
 */
@Component
@ConfigurationProperties(prefix = "yingxin.auth")
public class AuthProperties {

    /** 是否启用鉴权；仅本地排查问题时允许临时关闭，云端必须为 true */
    private boolean enabled = true;

    /** HS256 密钥，必须与 auth-service 一致、长度 ≥ 32 字节 */
    private String jwtSecret = "yingxin-local-dev-jwt-secret-change-me-32bytes";

    /** 额外放行路径（Ant 风格），默认空 */
    private List<String> extraWhitelist = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public List<String> getExtraWhitelist() {
        return extraWhitelist;
    }

    public void setExtraWhitelist(List<String> extraWhitelist) {
        this.extraWhitelist = extraWhitelist;
    }
}
