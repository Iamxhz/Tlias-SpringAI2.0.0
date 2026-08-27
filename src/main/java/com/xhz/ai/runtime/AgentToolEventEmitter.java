package com.xhz.ai.runtime;

import com.xhz.ai.dto.AgentEvent;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * Tool 可观测事件发射器：统一发 TOOL_START / TOOL_RESULT，避免每个 Tool 重复解析 ToolContext。
 */
@Component
public class AgentToolEventEmitter {

    private final AgentRunRegistry agentRunRegistry;

    public AgentToolEventEmitter(AgentRunRegistry agentRunRegistry) {
        this.agentRunRegistry = agentRunRegistry;
    }

    public void emitStart(ToolContext toolContext, String toolName, String summary) {
        AgentRunRegistry.RunHandle handle = resolve(toolContext);
        if (handle == null) {
            return;
        }
        handle.emit(AgentEvent.toolStart(handle.runId(), handle.conversationId(), toolName, summary));
    }

    public void emitResult(ToolContext toolContext, String toolName, String result, boolean success) {
        AgentRunRegistry.RunHandle handle = resolve(toolContext);
        if (handle == null) {
            return;
        }
        // 时间线只展示摘要，避免过长
        String brief = result == null ? "" : (result.length() > 120 ? result.substring(0, 120) + "…" : result);
        handle.emit(AgentEvent.toolResult(handle.runId(), handle.conversationId(), toolName, brief, success));
    }

    private AgentRunRegistry.RunHandle resolve(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object cid = toolContext.getContext().get(TeacherAgentRuntime.CTX_CONVERSATION_ID);
        if (cid == null) {
            return null;
        }
        return agentRunRegistry.get(String.valueOf(cid));
    }
}
