package com.xhz.ai.service.impl;

import com.xhz.ai.dto.AgentEvent;
import com.xhz.ai.runtime.TeacherAgentRuntime;
import com.xhz.ai.service.AgentChatService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 对话服务 — 委托 {@link TeacherAgentRuntime}。
 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final TeacherAgentRuntime teacherAgentRuntime;

    public AgentChatServiceImpl(TeacherAgentRuntime teacherAgentRuntime) {
        this.teacherAgentRuntime = teacherAgentRuntime;
    }

    @Override
    public Flux<AgentEvent> agentStreamChat(String message, String conversationId, String imageUrl, String imageBase64) {
        return teacherAgentRuntime.run(message, conversationId, imageUrl, imageBase64);
    }
}
