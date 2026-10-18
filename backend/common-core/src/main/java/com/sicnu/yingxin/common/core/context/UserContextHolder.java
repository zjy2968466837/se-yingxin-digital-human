package com.sicnu.yingxin.common.core.context;

/**
 * 用户上下文持有器（业务服务侧）。
 *
 * <p>由 common-web 的拦截器在请求进入 Controller 前写入，业务代码通过本类读取当前登录用户。
 * 请求结束后必须清理，避免线程复用导致上下文泄露。
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /** 获取当前上下文 */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /** 获取当前用户 ID，未登录返回 null */
    public static Long getUserId() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getUserId();
    }

    /** 设置上下文 */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /** 清理上下文，必须在拦截器 finally 中调用 */
    public static void clear() {
        CONTEXT.remove();
    }
}
