package com.sicnu.yingxin.common.core.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 统一响应体（规范 §3.2，H-01）。
 *
 * <p>所有 HTTP 接口都返回该结构，包括网关自身产生的响应（鉴权失败等），
 * 前端因此只需要判断 {@code code}，拦截器逻辑保持单一分支。
 *
 * <pre>
 *   { "code": 0, "message": "成功", "data": { ... } }
 * </pre>
 *
 * <p>约定：
 * <ul>
 *   <li>{@code code} 为 0 表示成功，非 0 见 {@link ErrorCode} 分段；</li>
 *   <li>{@code message} 可直接展示给用户，<b>不含堆栈、SQL、类名</b>；</li>
 *   <li>{@code data} 成功时为业务数据，失败时为 {@code null}。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Getter
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final T data;

    @JsonCreator
    private Result(@JsonProperty("code") int code,
                   @JsonProperty("message") String message,
                   @JsonProperty("data") T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /* ---------- 成功 ---------- */

    public static Result<Void> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /* ---------- 失败 ---------- */

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /** 使用服务专属段位的错误码 */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
