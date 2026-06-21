package com.xhz.service;

import reactor.core.publisher.Flux;

/**
 * AI 对话服务接口 — 洋葱架构业务逻辑层
 *
 * @see com.xhz.service.impl.AiServiceImpl
 */
public interface AiService {

    /**
     * 流式对话（Server-Sent Events）
     *
     * @param message        用户输入的自然语言消息
     * @param conversationId 会话 ID，用于隔离不同用户/会话的历史记忆
     * @return 大模型流式响应的 Flux 序列
     */
    Flux<String> agentStreamChat(String message, String conversationId);
}
