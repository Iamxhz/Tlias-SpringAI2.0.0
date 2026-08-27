package com.xhz.utils;

import java.util.List;

/**
 * 当前请求的安全上下文 — ThreadLocal 隔离。
 *
 * <p>由 {@link com.xhz.interceptor.TokenInterceptor} 在请求入口设置，
 * 由 {@link com.xhz.aspect.PermissionAspect} 在执行 @RequirePermission 方法时读取。
 * 请求结束后自动清除（ThreadLocal 生命周期绑定请求线程）。
 */
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
        // 工具类，禁止实例化
    }

    /** 设置当前请求的安全上下文 */
    public static void set(SecurityContext ctx) {
        CONTEXT.set(ctx);
    }

    /** 获取当前请求的安全上下文（可能为 null，表示未登录或白名单接口） */
    public static SecurityContext get() {
        return CONTEXT.get();
    }

    /** 请求结束时清除（防止线程池复用导致的内存泄漏） */
    public static void clear() {
        CONTEXT.remove();
    }

    // ==================== 安全上下文数据载体 ====================

    /**
     * @param userId      员工 ID
     * @param username    登录用户名
     * @param role        当前角色标识，如 ADMIN
     * @param permissions 当前角色拥有的权限码列表
     */
    public record SecurityContext(
            Integer userId,
            String username,
            String role,
            List<String> permissions
    ) {}
}
