package com.sicnu.yingxin.common.core.constant;

/**
 * 认证与上下文透传常量（规范 §4.2、§4.3）。
 *
 * <p>Header 名是网关与业务服务之间的固定契约，任何一方不得擅自增删或改名。
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** 请求头中的访问令牌：{@code Authorization: Bearer <token>} */
    public static final String AUTHORIZATION = "Authorization";

    /** Bearer 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** 网关注入的用户 ID */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 网关注入的用户角色 */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    /** 网关注入的链路追踪 ID */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** JWT 负载字段：用户 ID */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT 负载字段：角色 */
    public static final String CLAIM_ROLE = "role";

    /** JWT 负载字段：令牌唯一标识（用于 Redis 会话键） */
    public static final String CLAIM_JTI = "jti";

    /** 学生角色 */
    public static final String ROLE_STUDENT = "STUDENT";

    /** 后台管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";
}
