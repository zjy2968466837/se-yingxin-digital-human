package com.sicnu.yingxin.common.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档（IR-3.7）统一元信息：每个业务服务的 `/v3/api-docs` 都有可读的标题与描述，
 * 不再是一份 "OpenAPI definition" 的空白文档。
 *
 * <p>网关不引 common-web，因此不会加载本配置；服务可按需在 yml 覆盖
 * {@code yingxin.api-doc.title} / {@code yingxin.api-doc.description}。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI yingxinOpenAPI(
            @Value("${spring.application.name:service}") String serviceName,
            @Value("${yingxin.api-doc.title:}") String title,
            @Value("${yingxin.api-doc.description:}") String description) {
        String docTitle = title.isBlank() ? serviceName + " 接口文档" : title;
        String docDescription = description.isBlank()
                ? "大学生迎新导引服务数字人系统 · " + serviceName
                : description;
        return new OpenAPI().info(new Info()
                .title(docTitle)
                .version("1.0.0")
                .description(docDescription));
    }
}
