package com.xhz.interceptor;

import com.xhz.utils.SecurityContextHolder;
import com.xhz.utils.CurrentHolder;
import com.xhz.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * JWT 令牌拦截器 — Phase 2 增强版
 *
 * <p>职责：校验 JWT 合法性 + 提取用户上下文（userId、username、role、permissions）至 {@link SecurityContextHolder}。
 * 权限校验由 {@link com.xhz.system.security.PermissionAspect} 独立完成。
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求 url
        String url = request.getRequestURL().toString();

        // 2. 登录请求放行（原有 /login + 新增 /auth/login、/auth/refresh）
        if (url.contains("login")) {
            log.debug("登录/auth 请求，直接放行：{}", url);
            return true;
        }

        // 3. 获取请求头中的 token
        String jwt = request.getHeader("token");

        // 4. token 为空 → 未登录
        if (!StringUtils.hasLength(jwt)) {
            log.warn("请求 {} 缺少 token，拦截", url);
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }

        // 5. 解析 token → 提取用户上下文
        try {
            Claims claims = JwtUtils.parseJWT(jwt);

            Integer userId = claims.get("id", Integer.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) claims.get("permissions");

            // 设置安全上下文（供 PermissionAspect 使用）
            SecurityContextHolder.set(
                    new SecurityContextHolder.SecurityContext(userId, username, role, permissions)
            );

            // 向后兼容：旧代码（OperationLogAspect）仍从 CurrentHolder 取值
            CurrentHolder.setCurrentId(userId);

            log.debug("JWT 解析成功：userId={}, username={}, role={}, permissions={}",
                    userId, username, role, permissions);

        } catch (Exception e) {
            log.warn("解析 token 失败：{}", e.getMessage());
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }

        // 6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束，清理 ThreadLocal（防止线程池复用导致内存泄漏/数据串扰）
        SecurityContextHolder.clear();
        CurrentHolder.remove();
    }
}
