package com.sicnu.yingxin.common.web.advice;

import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理（H-02）：把异常统一转成 {@code {code, message, data}}，让前端只判断 code。
 *
 * <p>HTTP 状态码与 code 的对应关系（开发规范 §3.2）：
 * 2001–2099 / 4001–4099 → 200；3001–3099 → 400；1001–1099 → 401/403；9001–9099 → 500。
 * message 面向用户，**不含堆栈、SQL、类名**；完整异常只进日志（含 traceId）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：可预期失败，WARN 级别，不打堆栈。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常 code={} message={} traceId={}", e.getCode(), e.getMessage(), MDC.get("traceId"));
        return ResponseEntity.status(httpStatusOf(e.getCode()))
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 上的 Bean Validation 失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        ErrorCode code = codeOfValidation(fieldError);
        String message = fieldError == null ? code.getMessage() : fieldError.getDefaultMessage();
        log.warn("参数校验失败 field={} message={}", fieldError == null ? "-" : fieldError.getField(), message);
        return badRequest(Result.fail(code, message));
    }

    /** 表单/查询参数绑定校验失败。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        ErrorCode code = codeOfValidation(fieldError);
        String message = fieldError == null ? code.getMessage() : fieldError.getDefaultMessage();
        return badRequest(Result.fail(code, message));
    }

    /** 方法级校验（@Validated 在 Service 上）失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        ConstraintViolation<?> violation = e.getConstraintViolations().stream().findFirst().orElse(null);
        String message = violation == null ? ErrorCode.PARAM_FORMAT.getMessage() : violation.getMessage();
        ErrorCode code = violation == null ? ErrorCode.PARAM_FORMAT
                : codeOfAnnotation(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        return badRequest(Result.fail(code, message));
    }

    /** 缺少必填参数。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest(Result.fail(ErrorCode.PARAM_MISSING, "缺少必填参数：" + e.getParameterName()));
    }

    /** 参数类型不匹配（如把 abc 传给 id）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest(Result.fail(ErrorCode.PARAM_FORMAT, "参数格式不正确：" + e.getName()));
    }

    /** 请求体无法解析（JSON 语法错误、时间格式错误等）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return badRequest(Result.fail(ErrorCode.PARAM_FORMAT, "请求体格式不正确"));
    }

    /** 路径不存在（404 也要统一体，前端只判断 code）。 */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR, "请求的接口不存在"));
    }

    /** 方法不支持（405 也要统一体）。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR, "请求方法不被支持"));
    }

    /** 兜底：未捕获异常，ERROR 级别并打全堆栈（含 traceId），响应只给通用提示。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未捕获异常 traceId={}", MDC.get("traceId"), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    /** code 段位 → HTTP 状态码（传输语义）。 */
    static HttpStatus httpStatusOf(int code) {
        if (code >= 1001 && code <= 1099) {
            return code == ErrorCode.NO_PERMISSION.getCode() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        }
        if (code >= 3001 && code <= 3099) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 9001 && code <= 9099) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 2001–2099 业务失败、4001–4099 外部依赖降级：请求已被正确处理
        return HttpStatus.OK;
    }

    private static ResponseEntity<Result<Void>> badRequest(Result<Void> body) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private static ErrorCode codeOfValidation(FieldError fieldError) {
        return fieldError == null ? ErrorCode.PARAM_MISSING : codeOfAnnotation(fieldError.getCode());
    }

    /** 校验注解 → 参数段位的四个码（3001/3002/3003/3004）。 */
    private static ErrorCode codeOfAnnotation(String annotation) {
        if (annotation == null) {
            return ErrorCode.PARAM_FORMAT;
        }
        return switch (annotation) {
            case "NotNull", "NotBlank", "NotEmpty" -> ErrorCode.PARAM_MISSING;
            case "Size", "Length" -> ErrorCode.PARAM_LENGTH;
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero",
                 "Negative", "NegativeOrZero", "Range", "Digits" -> ErrorCode.PARAM_RANGE;
            default -> ErrorCode.PARAM_FORMAT;
        };
    }
}
