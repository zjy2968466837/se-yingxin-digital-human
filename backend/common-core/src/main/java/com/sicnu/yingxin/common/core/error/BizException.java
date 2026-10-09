package com.sicnu.yingxin.common.core.error;

/**
 * 业务异常（H-02）：**可预期的**业务失败用它表达，由 {@code GlobalExceptionHandler} 统一转成
 * {@code Result}，不打印堆栈、不返回 5xx。
 *
 * <p>纪律（开发规范 §2）：
 * <ul>
 *   <li>禁止用返回 {@code null} / {@code false} 表达失败，也禁止把业务失败包装成 RuntimeException 往上抛；</li>
 *   <li>message 必须是可以直接展示给用户的中文，不含 SQL、类名、字段名；</li>
 *   <li>服务专属码必须落在自己的段位内（见 {@link ServiceSegment}）。</li>
 * </ul>
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码（已登记的全局码或本服务段位内的码） */
    private final int code;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public BizException(int code, String message) {
        super(message, null, false, false);   // 业务异常不需要堆栈，省开销
        this.code = code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }

    /** 常用语义的快捷构造，避免各服务各写一套判断。 */
    public static BizException notFound(String what) {
        return new BizException(ErrorCode.RESOURCE_NOT_FOUND, what + "不存在或已被删除");
    }

    public static BizException stateNotAllowed(String message) {
        return new BizException(ErrorCode.STATE_NOT_ALLOWED, message);
    }

    public static BizException duplicateSubmit() {
        return new BizException(ErrorCode.DUPLICATE_SUBMIT);
    }

    public static BizException noPermission(String message) {
        return new BizException(ErrorCode.NO_PERMISSION, message);
    }

    public int getCode() {
        return code;
    }
}
