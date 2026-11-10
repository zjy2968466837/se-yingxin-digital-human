package com.sicnu.yingxin.common.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（H-07）：分页拦截器 + 防全表更新/删除；审计字段填充见 {@link AuditMetaObjectHandler}。
 *
 * <p>分页统一走 {@code Page}，Service 层再转 {@code PageResult}；禁止在 SQL 里手写 limit。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class MybatisPlusConfig {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 兜底：拦截没有 where 条件的 update/delete（防止误操作全表）
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
