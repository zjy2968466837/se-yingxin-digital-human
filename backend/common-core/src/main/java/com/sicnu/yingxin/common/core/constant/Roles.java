package com.sicnu.yingxin.common.core.constant;

/**
 * 角色取值口径（开发规范 §4.1）：令牌与 {@code X-User-Role} 头只使用这几个值，
 * 业务代码判断角色一律与常量比较，禁止裸写字符串。
 */
public final class Roles {

    /** 学生（含按组授权的教职工，区别仅在于权限点） */
    public static final String STUDENT = "STUDENT";

    /** 后台管理员 / 超级组 */
    public static final String ADMIN = "ADMIN";

    private Roles() {
    }
}
