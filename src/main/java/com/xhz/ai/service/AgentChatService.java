package com.xhz.ai.service;

import com.xhz.ai.dto.AgentEvent;
import reactor.core.publisher.Flux;

/**
 * 班主任 Agent 对话服务（只负责聊天流，审批见 {@link ApprovalService}）。
 */
public interface AgentChatService {

    Flux<AgentEvent> agentStreamChat(String message, String conversationId, String imageUrl, String imageBase64);
}
