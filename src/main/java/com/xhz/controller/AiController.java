package com.xhz.controller;

import com.xhz.pojo.ChatRequest;
import com.xhz.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 对话控制器 — Spring AI 2.0.0 洋葱架构路由控制层
 *
 * 职责：仅负责 HTTP 路由映射与请求参数提取。
 * 原则：Controller 极其轻薄，所有业务逻辑委托给 {@link AiService}。
 * 端点：POST /ai/chat，响应类型为 SSE（text/event-stream）
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 流式对话接口
     *
     * 会话 ID 当前固定为 tlias-admin-session-001，
     * 后续可改为从登录 Token 或请求头中动态提取。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChat(@RequestBody ChatRequest request) {
        String message = request != null ? request.getMessage() : null;
        // 会话 ID — 后续可改为从登录 Token 中动态提取
        return aiService.agentStreamChat(message, "tlias-admin-session-001");
    }
}
