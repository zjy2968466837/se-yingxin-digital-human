package com.sicnu.yingxin.auth.error;

/**
 * auth-service 专属错误码（规范 §3.3，2101–2199 段）。
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
}
