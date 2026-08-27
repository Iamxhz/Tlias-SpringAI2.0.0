package com.xhz.ai.runtime;

import com.xhz.ai.dto.AgentEvent;
import com.xhz.ai.dto.PendingApproval;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * 写操作 Tool 共用：解析运行上下文、推 APPROVAL_REQUIRED、返回待确认文案。
 */
@Component
public class HitlSupport {

    private final AgentRunRegistry agentRunRegistry;
    private final AgentToolEventEmitter toolEventEmitter;

    public HitlSupport(AgentRunRegistry agentRunRegistry, AgentToolEventEmitter toolEventEmitter) {
        this.agentRunRegistry = agentRunRegistry;
        this.toolEventEmitter = toolEventEmitter;
    }

    public String submit(ToolContext toolContext, String toolName, String startSummary,
                         java.util.function.BiFunction<String, String, PendingApproval> factory) {
        toolEventEmitter.emitStart(toolContext, toolName, startSummary);

        String runId = ctx(toolContext, TeacherAgentRuntime.CTX_RUN_ID);
        String conversationId = ctx(toolContext, TeacherAgentRuntime.CTX_CONVERSATION_ID);
        if (runId == null || conversationId == null) {
            String msg = "操作失败：缺少 Agent 运行上下文，无法发起确认。";
            toolEventEmitter.emitResult(toolContext, toolName, msg, false);
            return msg;
        }

        AgentRunRegistry.RunHandle handle = agentRunRegistry.get(conversationId);
        if (handle == null) {
            String msg = "操作失败：当前会话的 Agent 运行已结束，无法发起确认。";
            toolEventEmitter.emitResult(toolContext, toolName, msg, false);
            return msg;
        }

        try {
            PendingApproval pending = factory.apply(runId, conversationId);
            handle.emit(AgentEvent.approvalRequired(
                    runId, conversationId, pending.approvalId(),
                    pending.studentId(), pending.score(), toolName, startSummary));
            String result = String.format(
                    "已创建待确认指令（approvalId=%s）。数据库尚未修改。请提醒班主任在界面确认；确认前不要声称操作已成功。",
                    pending.approvalId());
            toolEventEmitter.emitResult(toolContext, toolName, result, true);
            return result;
        } catch (Exception e) {
            String msg = "发起确认失败：" + e.getMessage();
            toolEventEmitter.emitResult(toolContext, toolName, msg, false);
            return msg;
        }
    }

    private static String ctx(ToolContext toolContext, String key) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
