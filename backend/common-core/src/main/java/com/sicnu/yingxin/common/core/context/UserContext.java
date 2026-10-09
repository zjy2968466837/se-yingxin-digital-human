package com.sicnu.yingxin.common.core.context;

import java.io.Serializable;

/**
 * 当前请求的用户上下文（值对象，不含 Web 依赖）。
 *
 * <p>来源只有一个：网关解析令牌后写入 {@code X-User-Id} / {@code X-User-Role}，业务服务侧的
 * {@code UserContextFilter} 读出来放进线程上下文。业务代码**不得**直接从 Header 或 JWT 里取用户。
 */
public final class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 系统写入场景（定时任务、初始化数据）使用的用户 id */
    public static final long SYSTEM_USER_ID = 0L;

    private final Long userId;
    private final String role;
    private final String traceId;

    private UserContext(Long userId, String role, String traceId) {
        this.userId = userId;
        this.role = role;
        this.traceId = traceId;
    }

    public static UserContext of(Long userId, String role, String traceId) {
        return new UserContext(userId, role, traceId);
    }

    /** 未登录/系统任务场景 */
    public static UserContext anonymous(String traceId) {
        return new UserContext(null, null, traceId);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getTraceId() {
        return traceId;
    }

    @Override
    public String toString() {
        return "UserContext{userId=" + userId + ", role='" + role + "'}";
    }
}
