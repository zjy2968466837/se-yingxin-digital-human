package com.sicnu.yingxin.common.web.advice;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理器（规范 §3.2、§3.3，H-02 + H-03）。
 *
 * <p><b>为什么必须统一</b>：约束 C-4 要求公共基础设施先定统一方案再逐服务落地。
 * 若各服务自己写一套，同一个「资源不存在」，有的返回 500 堆栈、有的返回自定义结构，前端无法适配。
 *
 * <p>转换规则：
 * <ol>
 *   <li>业务异常 → 按 {@link ErrorCode#getHttpStatus()} 决定 HTTP 状态，业务码原样返回；</li>
 *   <li>参数校验失败 → 400 + 3xxx，message 必须指明具体字段（规范 §9 DoD 第 4 条）；</li>
 *   <li>其余未捕获异常 → 500 + 9001，<b>只记日志、不外泄异常原文</b>。</li>
 * </ol>
 *
 * <p><b>安全约定</b>：响应 message 一律使用预置文案，绝不回传 {@code e.getMessage()}——
 * 它可能包含表名、SQL 片段、内网地址。排查靠日志，前端只拿到错误码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ==================== 业务异常 ==================== */

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        // 业务失败是预期内分支，用 WARN，不用 ERROR，避免淹没真实缺陷
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(Result.failure(e.getCode(), e.getMessage()));
    }

    /* ==================== 参数校验 ==================== */

    /** 请求体校验失败：@RequestBody + @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return badRequest(firstFieldError(e.getBindingResult().getFieldErrors()));
    }

    /** 表单 / 查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        return badRequest(firstFieldError(e.getBindingResult().getFieldErrors()));
    }

    /** 方法参数上的约束校验失败：@RequestParam / @PathVariable + @Validated */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse(ErrorCode.PARAM_FORMAT.getMessage());
        return badRequest(message);
    }

    /** 请求体缺失或 JSON 格式非法 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return badRequest(ErrorCode.PARAM_FORMAT.getMessage());
    }

    /* ==================== HTTP 协议层 ==================== */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持 method={}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.failure(ErrorCode.PARAM_FORMAT.getCode(),
                        "请求方法 " + e.getMethod() + " 不被支持"));
    }

    /** Spring 6.1+ 静态资源 / 路径不存在，按规范 §3.2 归入 9xxx 段 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(ErrorCode.PATH_NOT_FOUND));
    }

    /* ==================== 兜底 ==================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        // 唯一记录堆栈的地方，供排查使用；响应体只给错误码与预置文案
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ErrorCode.SYSTEM_ERROR));
    }

    /* ==================== 私有方法 ==================== */

    private ResponseEntity<Result<Void>> badRequest(String message) {
        log.warn("参数校验失败 {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(ErrorCode.PARAM_REQUIRED.getCode(), message));
    }

    /** 取第一条字段错误，格式「字段名 错误提示」 */
    private String firstFieldError(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_REQUIRED.getMessage());
    }
}
