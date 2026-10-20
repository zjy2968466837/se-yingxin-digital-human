package com.sicnu.yingxin.common.core.context;

import lombok.Getter;

import java.io.Serializable;

/**
 * 用户上下文（规范 §4.3）。
 *
 * <p>网关解析 JWT 后通过固定 Header 注入下游，业务服务只读不解析。
 * 本类为不可变对象，禁止在业务代码中手动构造后写入请求头。
 */
@Getter
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private final Long userId;

    /** 角色，见 {@link com.sicnu.yingxin.common.core.constant.AuthConstants} */
    private final String role;

    /** 链路追踪 ID */
    private final String traceId;

    public UserContext(Long userId, String role, String traceId) {
        this.userId = userId;
        this.role = role;
        this.traceId = traceId;
    }

    /** 是否已登录 */
    public boolean isAuthenticated() {
        return userId != null && userId > 0;
    }

    /** 是否为学生 */
    public boolean isStudent() {
        return com.sicnu.yingxin.common.core.constant.AuthConstants.ROLE_STUDENT.equals(role);
    }

    /** 是否为管理员 */
    public boolean isAdmin() {
        return com.sicnu.yingxin.common.core.constant.AuthConstants.ROLE_ADMIN.equals(role);
    }
}
