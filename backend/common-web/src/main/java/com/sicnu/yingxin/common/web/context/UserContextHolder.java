package com.sicnu.yingxin.common.web.context;

import com.sicnu.yingxin.common.core.context.UserContext;

/**
 * 用户上下文的线程持有者（H-05）。
 *
 * <p>由 {@code UserContextFilter} 在请求进入时写入、请求结束时清除。
 * 业务代码取当前用户只走这里：{@code UserContextHolder.getUserId()}。
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    /** 未登录时返回 null（不抛异常，由调用方决定是否要求登录）。 */
    public static Long getUserId() {
        UserContext context = HOLDER.get();
        return context == null ? null : context.getUserId();
    }

    /** 需要「当前操作人 id」的场景（审计字段填充）：未登录时取系统用户 0。 */
    public static Long getUserIdOrSystem() {
        Long userId = getUserId();
        return userId == null ? UserContext.SYSTEM_USER_ID : userId;
    }

    public static String getRole() {
        UserContext context = HOLDER.get();
        return context == null ? null : context.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private UserContextHolder() {
    }
}
