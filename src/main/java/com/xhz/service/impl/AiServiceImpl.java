package com.xhz.service.impl;

import com.xhz.service.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务实现 — Spring AI 2.0.0 洋葱架构业务逻辑层核心
 *
 * 职责：仅负责设置请求级 conversationId 并触发流式调用。
 *
 * ChatClient 的全部全局装配（RAG、Memory、Tools、System Prompt）
 * 已由 {@link com.xhz.aiconfig.ChatClientConfig} 完成，
 * 此处无需再关心 ChatClient.Builder、VectorStore 等基础设施细节。
 *
 * 依赖注入说明：当容器中存在多个 ChatClient Bean 时，
 * Spring 按类型（byType）匹配失败后自动退化为按名称（byName）匹配：
 * {@code agentChatClient} → ChatClientConfig.agentChatClient()，
 * {@code ChatClient111} → ChatClientConfig.ChatClient111()。
 */
@Service
public class AiServiceImpl implements AiService {

    /** 已全局装配的 ChatClient（含 RAG + Memory + Tools） */
    private final ChatClient agentChatClient;

    /** 无装配的备用 ChatClient（测试用） */
    private final ChatClient ChatClient111;

    @Autowired
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    public AiServiceImpl(ChatClient agentChatClient,
                         ChatClient ChatClient111) {
        this.agentChatClient = agentChatClient;
        this.ChatClient111 = ChatClient111;
    }

    /**
     * 流式对话 — 仅设置 conversationId，其余全部由全局 ChatClient 处理
     */
    @Override
    public Flux<String> agentStreamChat(String message, String conversationId) {
        if (message == null || message.isBlank()) {
            return Flux.just("请输入有效的消息内容。");
        }

        return agentChatClient.prompt()
                .user(message)
                .system("用户询问天气时，你必须调用工具查询后再用简短中文回答。")

                // 请求级参数：传入会话 ID 以隔离不同用户/会话的对话历史
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
