package com.xhz.aspect;

import com.xhz.ai.runtime.TeacherAgentRuntime;
import com.xhz.anno.RequirePermission;
import com.xhz.exception.AccessDeniedException;
import com.xhz.utils.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 权限校验 AOP 切面 — 拦截 {@link RequirePermission} 注解的方法。
 *
 * <p>REST 无权限时抛 {@link AccessDeniedException}；
 * AI {@link Tool} 无权限时返回自然语言拒绝语，避免打断 SSE。
 * SSE 场景下会从 {@link ToolContext} 恢复 {@link SecurityContextHolder}。
 */
@Slf4j
@Aspect
@Order(200)
@Component
public class PermissionAspect {

    @Around("@annotation(rp)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission rp) throws Throwable {
        String requiredPerm = rp.value();
        boolean restored = false;
        SecurityContextHolder.SecurityContext ctx = SecurityContextHolder.get();
        if (ctx == null) {
            ctx = securityFromToolArgs(joinPoint.getArgs());
            if (ctx != null) {
                SecurityContextHolder.set(ctx);
                restored = true;
            }
        }

        try {
            if (ctx == null) {
                log.warn("权限拦截：未获取到用户上下文，拒绝访问 [{}]", requiredPerm);
                return deny(joinPoint, requiredPerm, true);
            }

            List<String> userPerms = ctx.permissions();
            if (userPerms == null || userPerms.isEmpty()) {
                log.warn("权限拦截：用户 {} 无任何权限，需要 [{}]", ctx.username(), requiredPerm);
                return deny(joinPoint, requiredPerm, false);
            }

            boolean hasPermission = userPerms.stream().anyMatch(p -> matches(p, requiredPerm));
            if (!hasPermission) {
                log.warn("权限拦截：用户 {}（角色={}）缺少权限 [{}]", ctx.username(), ctx.role(), requiredPerm);
                return deny(joinPoint, requiredPerm, false);
            }

            log.debug("权限校验通过：用户 {} 执行 [{}]", ctx.username(), requiredPerm);
            return joinPoint.proceed();
        } finally {
            if (restored) {
                SecurityContextHolder.clear();
            }
        }
    }

    private Object deny(ProceedingJoinPoint joinPoint, String requiredPerm, boolean missingContext)
            throws AccessDeniedException {
        if (isAiTool(joinPoint)) {
            if (missingContext) {
                return "操作被拒绝：未识别到登录用户，无法执行需要权限 [" + requiredPerm + "] 的操作。";
            }
            return "操作被拒绝：当前用户缺少权限 [" + requiredPerm + "]，无法执行此操作。";
        }
        throw AccessDeniedException.missing(requiredPerm);
    }

    private static SecurityContextHolder.SecurityContext securityFromToolArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof ToolContext toolContext && toolContext.getContext() != null) {
                Object security = toolContext.getContext().get(TeacherAgentRuntime.CTX_SECURITY);
                if (security instanceof SecurityContextHolder.SecurityContext ctx) {
                    return ctx;
                }
            }
        }
        return null;
    }

    private static boolean isAiTool(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (AnnotationUtils.findAnnotation(method, Tool.class) != null) {
            return true;
        }
        try {
            Method targetMethod = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
            return AnnotationUtils.findAnnotation(targetMethod, Tool.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 权限码匹配 — 支持通配符 {@code *}。
     */
    private boolean matches(String userPerm, String requiredPerm) {
        if (userPerm.equals(requiredPerm)) {
            return true;
        }
        String regex = userPerm.replace("*", ".*");
        return requiredPerm.matches(regex);
    }
}
