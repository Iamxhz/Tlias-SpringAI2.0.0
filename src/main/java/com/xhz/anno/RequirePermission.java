package com.xhz.anno;

import com.xhz.aspect.PermissionAspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限校验注解 — 标注在 Controller 方法上。
 *
 * <p>使用示例：
 * <pre>{@code
 * @RequirePermission("emp:delete")
 * @DeleteMapping
 * public Result<Void> delete(@RequestParam List<Integer> ids) { ... }
 * }</pre>
 *
 * <p>权限码与 {@code sys_role_permission.perm_code} 匹配，
 * {@link PermissionAspect} 支持通配符 {@code *}（如 {@code emp:*} 匹配所有 emp 操作）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** 权限码，如 "emp:delete"、"dept:*" */
    String value();
}
