package com.sicnu.yingxin.common.web.config;

import com.sicnu.yingxin.common.web.advice.GlobalExceptionHandler;
import com.sicnu.yingxin.common.web.filter.TraceIdFilter;
import com.sicnu.yingxin.common.web.filter.UserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * common-web 自动装配：业务服务只要在 pom 里引 {@code common-web}，无需任何注解即获得统一形态
 * （需求分析文档 §2.6.2：服务的「运行基线 + 业务代码」，形态由公共模块保证，避免 9 个服务各写一套）。
 *
 * <p>网关（WebFlux）**不引本模块**，只引 {@code common-core} 用于 401 的统一响应体。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        GlobalExceptionHandler.class,
        JacksonConfig.class,
        MybatisPlusConfig.class,
        AuditMetaObjectHandler.class,
        RedisConfig.class,
})
public class CommonWebAutoConfiguration {

    /** traceId 必须最早生效，之后的日志（含异常日志）都能带上它。 */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /** 用户上下文紧随 traceId 之后（业务代码取 userId 依赖它）。 */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>(new UserContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
