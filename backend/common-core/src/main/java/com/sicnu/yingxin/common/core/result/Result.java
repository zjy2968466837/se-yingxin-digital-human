package com.sicnu.yingxin.common.core.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sicnu.yingxin.common.core.error.ErrorCode;

import java.io.Serializable;

/**
 * 统一响应体（IR-3.3 / 开发规范 §3.2）。
 *
 * <p>所有接口——包括异常路径与网关自身产生的响应——都是 {@code {code, message, data}}：
 * <ul>
 *   <li>{@code code}：0 表示成功，非 0 见错误码分段（开发规范 §3.3）；</li>
 *   <li>{@code message}：可直接展示给用户的中文提示，不含堆栈、SQL、类名；</li>
 *   <li>{@code data}：成功时的业务数据，失败时为 null——**字段必须存在**（前端只判断 code）。</li>
 * </ul>
 *
 * <p>HTTP 状态码只表达传输语义（400 参数、401/403 认证、5xx 系统异常），业务失败用 200 + code 表达。
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功且无数据（例如删除、退出登录）。 */
    public static Result<Void> ok() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /** 成功并返回数据。 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /** 成功并自定义提示语（数据仍返回）。 */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /** 失败：使用登记的错误码（推荐，message 取登记文案）。 */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败：使用登记的错误码 + 更具体的提示语（例如「学号已存在」）。 */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /** 失败：直接给码（服务专属段位 21xx–28xx 用这个，码值必须在本文登记的段位内）。 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 供 Java 代码判断，**不参与 JSON 序列化**（响应体固定只有 code / message / data 三个字段）。 */
    @JsonIgnore
    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{code=" + code + ", message='" + message + "'}";
    }
}
