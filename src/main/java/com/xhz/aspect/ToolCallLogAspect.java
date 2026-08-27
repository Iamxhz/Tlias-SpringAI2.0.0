package com.xhz.aspect;

import com.xhz.ai.runtime.TeacherAgentRuntime;
import com.xhz.utils.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 记录 AI {@code @Tool} 调用：入参、结果、耗时、当前用户与运行身份。
 */
@Slf4j
@Aspect
@Order(100)
@Component
public class ToolCallLogAspect {

    private static final int RESULT_LIMIT = 200;

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String user = currentUsername();
        String runId = ctxValue(args, TeacherAgentRuntime.CTX_RUN_ID);
        String conversationId = ctxValue(args, TeacherAgentRuntime.CTX_CONVERSATION_ID);
        String params = stringifyArgs(args);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("TOOL_CALL tool={} user={} runId={} conversationId={} args={} success=true cost={}ms result={}",
                    toolName, user, runId, conversationId, params, cost, brief(result));
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.error("TOOL_CALL tool={} user={} runId={} conversationId={} args={} success=false cost={}ms error={}",
                    toolName, user, runId, conversationId, params, cost, e.getMessage(), e);
            throw e;
        }
    }

    private static String currentUsername() {
        SecurityContextHolder.SecurityContext ctx = SecurityContextHolder.get();
        return ctx == null || ctx.username() == null ? "-" : ctx.username();
    }

    private static String ctxValue(Object[] args, String key) {
        ToolContext toolContext = findToolContext(args);
        if (toolContext == null || toolContext.getContext() == null) {
            return "-";
        }
        Map<String, Object> context = toolContext.getContext();
        Object value = context.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private static ToolContext findToolContext(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof ToolContext toolContext) {
                return toolContext;
            }
        }
        return null;
    }

    private static String stringifyArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof ToolContext) {
                continue;
            }
            parts.add(String.valueOf(arg));
        }
        return parts.toString();
    }

    private static String brief(Object result) {
        String text = String.valueOf(result);
        if (text.length() <= RESULT_LIMIT) {
            return text;
        }
        return text.substring(0, RESULT_LIMIT) + "…";
    }
}
