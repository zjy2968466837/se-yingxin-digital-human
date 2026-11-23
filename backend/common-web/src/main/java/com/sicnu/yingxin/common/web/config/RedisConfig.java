package com.sicnu.yingxin.common.web.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置（H-08 / 开发规范 §5.4）：key 用字符串、**value 一律 JSON 序列化**（禁用默认 JDK 序列化，
 * 否则 Redis 里是乱码，也无法跨语言排查）。
 *
 * <p>配套纪律：key 统一 {@code yx:<服务>:<业务>:<标识>}（见 {@code CacheKeys}），**必须设 TTL**，
 * 数据变更由写入方主动失效。
 *
 * <p>序列化器必须自己装配 ObjectMapper：{@code GenericJackson2JsonRedisSerializer} 的默认构造器
 * <b>不注册</b> JavaTimeModule，缓存 DTO 里只要有 {@code LocalDateTime} 就会写失败
 * （报 "Java 8 date/time type ... not supported by default"）。S5 打样时实测踩到过。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisTemplate.class)
public class RedisConfig {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = jsonSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存值序列化器：JSON + 类型信息 + Java 8 时间模块。
     *
     * <p>类型信息是 {@code GenericJackson2JsonRedisSerializer} 反序列化回具体类型的前提
     * （默认构造器同样开启，这里保持一致）。缓存只存本系统写入的对象，不存在外部不可信输入。
     */
    static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
