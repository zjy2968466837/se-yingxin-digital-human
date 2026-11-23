package com.sicnu.yingxin.auth.error;

/**
 * auth-service 专属错误码（开发规范 §3.3，2101–2199 段；可用 ServiceSegment.AUTH.contains 自检）。
 */
public final class AuthErrorCode {

    private AuthErrorCode() {
    }

    /** 账号不存在 */
    public static final int ACCOUNT_NOT_FOUND = 2101;

    /** 账号或密码错误 */
    public static final int BAD_CREDENTIALS = 2102;

    /** 账号已被禁用 */
    public static final int ACCOUNT_DISABLED = 2103;

    /** 登录类型不合法 */
    public static final int LOGIN_TYPE_INVALID = 2104;

    /** 用户组不存在 */
    public static final int GROUP_NOT_FOUND = 2105;

    /** 账号已存在 */
    public static final int ACCOUNT_EXISTS = 2106;

    /** 账号待审批 */
    public static final int APPROVE_REQUIRED = 2107;

    /** 内置数据不允许修改 */
    public static final int BUILTIN_PROTECTED = 2108;

    /** 账号已临时锁定 */
    public static final int ACCOUNT_LOCKED = 2109;

    /** 目标为超级管理员账号，不允许操作 */
    public static final int SUPER_ADMIN_PROTECTED = 2110;

    /** 令牌缺失或无效 */
    public static final int TOKEN_REQUIRED = 2111;
}
