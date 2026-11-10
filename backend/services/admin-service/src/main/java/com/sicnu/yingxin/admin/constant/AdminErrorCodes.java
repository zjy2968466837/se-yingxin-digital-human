package com.sicnu.yingxin.admin.constant;

/**
 * admin-service 专属错误码（段位 2801–2899，见《开发规范》§3.3）。
 *
 * <p>2801 已被"配置取值越界"占用，验证码相关从 2802 起。
 */
public final class AdminErrorCodes {

    /** 图形验证码错误或已过期 */
    public static final int CAPTCHA_INVALID = 2802;

    /** 连续登录失败达到阈值，账号临时锁定 */
    public static final int LOGIN_LOCKED = 2803;

    private AdminErrorCodes() {
    }
}
