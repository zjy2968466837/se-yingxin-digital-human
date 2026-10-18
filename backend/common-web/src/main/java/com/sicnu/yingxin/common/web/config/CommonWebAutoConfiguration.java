package com.sicnu.yingxin.common.web.config;

import com.sicnu.yingxin.common.web.advice.GlobalExceptionHandler;
import com.sicnu.yingxin.common.web.context.UserContextInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * common-web 自动装配入口。
 *
 * <p>业务服务只需在 pom 中引入 common-web 依赖，全局异常处理与用户上下文拦截即自动生效，
 * 无需在每个服务里写 {@code @ComponentScan} 或重复注册 —— 这是约束 C-4
 * 「不得各服务自建」在工程侧的落地方式。
 *
 * <p>所有 Bean 均标注 {@link ConditionalOnMissingBean}，服务可按需覆盖默认实现。
 */
@AutoConfiguration
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public WebMvcConfigurer userContextWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor())
                        // 放行 ping 与公开接口，与网关白名单保持一致
                        .addPathPatterns("/**")
                        .excludePathPatterns("/api/*/ping", "/api/public/**", "/actuator/**", "/swagger-ui.html", "/v3/api-docs/**");
            }
        };
    }
}
