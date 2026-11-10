package com.sicnu.yingxin.common.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * JSON 序列化配置（开发规范 §3.2 / §5.2）：
 * 时间统一 {@code yyyy-MM-dd HH:mm:ss}、时区 {@code Asia/Shanghai}，且**不过滤 null 字段**——
 * 统一响应体的 {@code data} 在失败时必须是显式的 null，否则前端判断分支会变复杂。
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return builder -> builder
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .serializationInclusion(JsonInclude.Include.ALWAYS)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTime))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime))
                .serializerByType(LocalDate.class, new LocalDateSerializer(date))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(date));
    }
}
