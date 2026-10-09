package com.sicnu.yingxin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码散列（数据建模 §3.1：{@code password_hash} 用 BCrypt）——只引 spring-security-crypto，
 * 不引整个 Spring Security（本项目鉴权在网关，不做方法级安全）。
 */
@Configuration(proxyBeanMethods = false)
public class AuthConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
