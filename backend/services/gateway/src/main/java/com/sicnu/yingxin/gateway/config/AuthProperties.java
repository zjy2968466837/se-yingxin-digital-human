package com.sicnu.yingxin.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权白名单（规范 §4.2）。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "yingxin.gateway.auth")
public class AuthProperties {

    /** 免鉴权路径列表 */
    private List<String> whitelist = new ArrayList<>();
}
