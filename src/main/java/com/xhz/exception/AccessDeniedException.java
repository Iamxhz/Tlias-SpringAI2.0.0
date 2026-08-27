package com.xhz.exception;

/**
 * 权限不足异常 — 当用户尝试访问未授权的接口时抛出。
 *
 * <p>由 {@link com.xhz.common.security.RequirePermission} AOP 切面自动抛出，
 * 由 {@link GlobalExceptionHandler} 统一拦截并返回友好提示。
 *
 * <p>当前为 Phase 1 预置异常类，Phase 2 RBAC 模块将正式接入。
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    /**
     * 快捷构造：提示缺少指定权限码。
     *
     * @param permissionCode 缺失的权限码，如 "emp:delete"
     */
    public static AccessDeniedException missing(String permissionCode) {
        return new AccessDeniedException("权限不足，缺少权限码：" + permissionCode);
    }
}
