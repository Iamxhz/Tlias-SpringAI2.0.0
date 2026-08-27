package com.xhz.exception;

import com.xhz.pojo.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理切面 — 覆盖参数校验、业务异常、权限异常、系统异常 6 类场景。
 *
 * <p>所有异常统一返回 {@link Result} 格式，HTTP 状态码通过 @ResponseStatus 区分。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 参数校验异常 ====================

    /**
     * @Valid / @Validated 校验 @RequestBody 失败时触发。
     * 返回字段级错误拼接，形如 "name: 姓名不能为空; phone: 手机号格式错误"。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求参数校验失败: {}", errors);
        return Result.fail(errors);
    }

    /**
     * @Validated 校验类级别或方法参数（非 @RequestBody）失败时触发。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", errors);
        return Result.fail(errors);
    }

    // ==================== 请求格式异常 ====================

    /**
     * JSON 格式错误或请求体不可解析（如字段类型不匹配）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail("请求数据格式错误，请检查 JSON 结构或字段类型。");
    }

    /**
     * 缺少必填的 @RequestParam 参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填请求参数: {}", e.getParameterName());
        return Result.fail("缺少必填参数：" + e.getParameterName());
    }

    // ==================== 业务异常 ====================

    /**
     * 自定义业务异常 — 预期内的业务规则拦截。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.info("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 权限不足异常 — 由 RequirePermission AOP 切面自动抛出（Phase 2 正式接入）。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    // ==================== 兜底异常 ====================

    /**
     * 系统级兜底 — 处理所有未被上述 Handler 捕获的异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("对不起，操作失败，请联系管理员。");
    }
}