package com.sicnu.yingxin.common.core.error;

/**
 * 业务异常。
 *
 * <p>用于表达<b>可预期的业务失败</b>（资源不存在、状态不允许、重复提交等），
 * 由 common-web 的全局异常处理器统一转换为响应体，业务代码不需要自己拼错误响应。
 *
 * <p>按规范 §2.3：业务可预期失败必须抛本异常，<b>禁止用返回 null / false 表达失败</b>。
 *
 * <p>不可预期的错误（空指针、SQL 异常等）不要包装成本异常抛出，让它们冒泡到全局处理器按系统错误处理，
 * 否则会把真实缺陷伪装成业务提示，线上难以定位。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码，取值见 {@link ErrorCode} 或各服务专属段位 */
    private final int code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, errorCode.getHttpStatus());
    }

    /**
     * 使用服务专属段位的错误码时走这个构造。
     * HTTP 状态默认 200（业务失败），需要 401 / 403 等语义时用四参构造。
     */
    public BizException(int code, String message) {
        this(code, message, 200);
    }

    public BizException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
