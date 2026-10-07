package com.sicnu.yingxin.common.core.error;

/**
 * 全局错误码。
 *
 * <p>分段规则见 docs/开发规范.md §3.3「段位即所有权」：
 * 一个服务只允许使用自己段位内的业务码，新增错误码须先在规范附录登记，避免撞码。
 *
 * <pre>
 *   0            成功
 *   1001-1099    认证与会话（全局）
 *   2001-2099    通用业务（全局）
 *   2101-2899    各服务专属段位，定义在各自服务的 error 包，不在此处
 *   3001-3099    参数与校验（全局）
 *   4001-4099    外部依赖降级
 *   9001-9099    系统错误
 * </pre>
 *
 * <p>{@code httpStatus} 只表达传输语义（规范 §3.2）：业务失败返回 200，入参非法返回 400，
 * 外部依赖降级同样返回 200——降级是预期内行为，不阻断主流程。
 */
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, "成功", 200),

    /* ============ 1001-1099 认证与会话（全局） ============ */
    UNAUTHORIZED(1001, "未登录或登录已失效", 401),
    TOKEN_INVALID(1002, "令牌无效或已过期", 401),
    BAD_CREDENTIALS(1003, "账号或密码错误", 200),
    ACCOUNT_DISABLED(1004, "账号已被禁用", 403),
    NO_PERMISSION(1005, "无权限访问", 403),

    /* ============ 2001-2099 通用业务（全局） ============ */
    RESOURCE_NOT_FOUND(2001, "资源不存在", 200),
    STATE_NOT_ALLOWED(2002, "当前状态不允许该操作", 200),
    DUPLICATE_SUBMIT(2003, "请勿重复提交", 200),
    DATA_CONFLICT(2004, "数据冲突，请刷新后重试", 200),

    /* ============ 3001-3099 参数与校验（全局） ============ */
    PARAM_REQUIRED(3001, "必填参数缺失", 400),
    PARAM_FORMAT(3002, "参数格式非法", 400),
    PARAM_OUT_OF_RANGE(3003, "参数取值越界", 400),
    PARAM_LENGTH(3004, "参数长度超出限制", 400),

    /* ============ 4001-4099 外部依赖降级 ============ */
    DEP_FINANCE(4001, "缴费服务暂不可用", 200),
    DEP_MAP(4002, "地图服务暂不可用", 200),
    DEP_LLM(4003, "智能问答服务暂不可用", 200),
    DEP_SPEECH(4004, "语音服务暂不可用", 200),
    DEP_WECHAT(4005, "微信接口暂不可用", 200),
    DEP_IDENTITY(4006, "学校身份认证服务暂不可用", 200),

    /* ============ 9001-9099 系统错误 ============ */
    SYSTEM_ERROR(9001, "系统繁忙，请稍后重试", 500),
    DB_ERROR(9002, "数据访问异常", 500),
    CACHE_ERROR(9003, "缓存服务异常", 500),
    RATE_LIMITED(9004, "请求过于频繁，请稍后重试", 429),
    DOWNSTREAM_UNAVAILABLE(9005, "下游服务不可达", 503),
    /** 规范 §3.2 约定 404 路径不存在归入 9xxx 段 */
    PATH_NOT_FOUND(9006, "请求路径不存在", 404),
    ;

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
