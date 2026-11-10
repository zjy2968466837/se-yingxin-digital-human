package com.sicnu.yingxin.common.core.constant;

/**
 * 网关注入的请求头名字（开发规范 §4.3，**固定名字，不得增删**）。
 *
 * <p>安全前提：网关必须先删除入站请求中的同名头再注入，业务服务只读这些头，
 * 禁止自行解析 JWT、禁止自行校验令牌。
 */
public final class HeaderNames {

    /** 当前用户 id（未登录时不存在） */
    public static final String USER_ID = "X-User-Id";

    /** 当前用户角色（如 STUDENT / ADMIN / VOLUNTEER / VISITOR） */
    public static final String USER_ROLE = "X-User-Role";

    /** 全链路追踪 id：网关生成，透传到网关与所有下游服务的日志 */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 前端带令牌的标准头（网关读取，业务服务不读） */
    public static final String AUTHORIZATION = "Authorization";

    /** 令牌前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    private HeaderNames() {
    }
}
