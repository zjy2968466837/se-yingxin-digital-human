package com.sicnu.yingxin.common.core.error;

/**
 * 错误码登记表（开发规范 §3.3）。
 *
 * <p>段位即所有权：一个服务只允许使用自己段位内的业务码（21xx–28xx，见 {@link ServiceSegment}）；
 * 新增任何码都必须先在《开发规范》附录登记，避免撞码。
 * 本类只承载**全局段位**中已经登记的码，服务专属码通过 {@code Result.fail(int, String)} 或
 * {@code BizException(int, String)} 使用，并保持在本服务段位内。
 */
public enum ErrorCode {

    SUCCESS(0, "成功"),

    // ---------- 1001–1099 认证与会话（全局） ----------
    UNAUTHORIZED(1001, "未登录"),
    TOKEN_INVALID(1002, "令牌无效或过期"),
    BAD_CREDENTIALS(1003, "账号或密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    NO_PERMISSION(1005, "无权限访问"),

    // ---------- 2001–2099 通用业务错误（全局） ----------
    RESOURCE_NOT_FOUND(2001, "资源不存在"),
    STATE_NOT_ALLOWED(2002, "当前状态不允许该操作"),
    DUPLICATE_SUBMIT(2003, "请勿重复提交"),
    DATA_CONFLICT(2004, "数据冲突"),

    // ---------- 3001–3099 参数与校验（全局） ----------
    PARAM_MISSING(3001, "必填参数缺失"),
    PARAM_FORMAT(3002, "参数格式不正确"),
    PARAM_RANGE(3003, "参数取值越界"),
    PARAM_LENGTH(3004, "参数长度超限"),

    // ---------- 4001–4099 外部依赖降级（前端展示降级提示，不阻断主流程） ----------
    DEP_FINANCE(4001, "缴费系统暂时不可用，稍后可重试"),
    DEP_MAP(4002, "地图服务暂时不可用"),
    DEP_LLM(4003, "智能问答暂不可用，已用问答库结果回答"),
    DEP_ASR_TTS(4004, "语音服务暂时不可用，可改用文字提问"),
    DEP_WECHAT(4005, "微信接口暂时不可用"),
    DEP_IDENTITY_SOURCE(4006, "学校身份源暂时不可用"),

    // ---------- 9001–9099 系统错误（全局 + 网关） ----------
    SYSTEM_ERROR(9001, "系统繁忙，请稍后重试"),
    DB_ERROR(9002, "数据库操作失败"),
    CACHE_ERROR(9003, "缓存操作失败"),
    RATE_LIMITED(9004, "请求过于频繁，请稍后重试"),
    DOWNSTREAM_UNAVAILABLE(9005, "下游服务不可达");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /** 反查：未登记的码返回 {@link #SYSTEM_ERROR}（避免抛异常掩盖真实错误）。 */
    public static ErrorCode of(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return SYSTEM_ERROR;
    }

    public static boolean isSuccess(int code) {
        return code == SUCCESS.code;
    }
}
