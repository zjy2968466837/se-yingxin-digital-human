package com.sicnu.yingxin.common.web.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 事件发布器自动装配：仅当 classpath 同时存在 spring-rabbit 与 spring-jdbc 时生效。
 *
 * <p>条件用 name 形式声明，避免在条件判断阶段加载第三方类导致 NoClassDefFoundError。
 * 未引入 spring-boot-starter-amqp 的服务不会创建该 Bean，也不应调用 {@link EventPublisher}。
 */
@AutoConfiguration
@ConditionalOnClass(name = {"org.springframework.amqp.rabbit.core.RabbitTemplate",
        "org.springframework.jdbc.core.JdbcTemplate"})
public class EventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                                         ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                         ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                         ObjectMapper objectMapper) {
        return new EventPublisher(rabbitTemplateProvider, jdbcTemplateProvider,
                redisTemplateProvider, objectMapper);
    }
}
