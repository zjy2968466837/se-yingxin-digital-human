package com.sicnu.yingxin.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 装配冒烟测试：不连 MySQL / Redis（连接都是懒加载），只验证自动装配没有冲突——
 * 特别是 {@code PasswordEncoder}、{@code RestClient.Builder} 与 {@code @ConfigurationProperties} 绑定。
 */
@SpringBootTest
class AdminServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
