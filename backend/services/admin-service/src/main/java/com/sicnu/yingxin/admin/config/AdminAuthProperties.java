package com.sicnu.yingxin.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 管理后台登录配置（FR-9.1）。
 *
 * <p>三个默认值只在本地有效：{@code AUTH_INTERNAL_SECRET} 必须与 auth-service 一致；
 * 生产环境由环境变量注入，不得沿用仓库里的占位串（开发规范 §4.1）。
 *
 * <p>{@code loginFailLockTimes} 对应《需求分析文档》§7.1.4 的 {@code auth.login.fail.lock.times}，
 * 当前从 yml / 环境变量读取；F9.10 的 {@code admin_config} 落地后再改为读配置表。
 */
@Data
@Component
@ConfigurationProperties(prefix = "yingxin.auth")
public class AdminAuthProperties {

    /** 服务间内部接口凭证（admin-service → auth-service 签发令牌） */
    private String internalSecret = "yingxin-local-dev-internal-secret-change-me";

    /** auth-service 内部签发接口地址 */
    private String tokenIssueUrl = "http://localhost:8100/api/auth/internal/token";

    /** 图形验证码有效期 */
    private Duration captchaTtl = Duration.ofMinutes(5);

    /** 登录失败计数窗口（也是锁定时长） */
    private Duration loginFailWindow = Duration.ofMinutes(10);

    /** 连续失败锁定阈值：默认 5，允许范围 3–10 */
    private int loginFailLockTimes = 5;

    /** BCrypt 代价因子：默认 12，允许范围 10–14（开发规范 §5.5） */
    private int bcryptCost = 12;
}
