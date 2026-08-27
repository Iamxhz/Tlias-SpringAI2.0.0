package com.xhz.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 班主任 Agent 推给前端的结构化 SSE 事件。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(
        AgentEventType type,
        String runId,
        String conversationId,
        String content,
        long timestamp,
        String approvalId,
        Integer studentId,
        Integer score,
        String toolName
) {

    public static AgentEvent started(String runId, String conversationId) {
        return new AgentEvent(AgentEventType.RUN_STARTED, runId, conversationId, null,
                System.currentTimeMillis(), null, null, null, null);
    }

    public static AgentEvent delta(String runId, String conversationId, String content) {
        return new AgentEvent(AgentEventType.DELTA, runId, conversationId, content,
                System.currentTimeMillis(), null, null, null, null);
    }

    public static AgentEvent toolStart(String runId, String conversationId, String toolName, String summary) {
        return new AgentEvent(AgentEventType.TOOL_START, runId, conversationId, summary,
                System.currentTimeMillis(), null, null, null, toolName);
    }

    public static AgentEvent toolResult(String runId, String conversationId, String toolName,
                                        String result, boolean success) {
        String content = (success ? "✓ " : "✗ ") + result;
        return new AgentEvent(AgentEventType.TOOL_RESULT, runId, conversationId, content,
                System.currentTimeMillis(), null, null, null, toolName);
    }

    public static AgentEvent finished(String runId, String conversationId) {
        return new AgentEvent(AgentEventType.RUN_FINISHED, runId, conversationId, null,
                System.currentTimeMillis(), null, null, null, null);
    }

    public static AgentEvent error(String runId, String conversationId, String message) {
        return new AgentEvent(AgentEventType.ERROR, runId, conversationId, message,
                System.currentTimeMillis(), null, null, null, null);
    }

    public static AgentEvent visionResult(String runId, String conversationId, String summary) {
        return new AgentEvent(AgentEventType.VISION_RESULT, runId, conversationId, summary,
                System.currentTimeMillis(), null, null, null, "analyzeViolationEvidence");
    }

    public static AgentEvent approvalRequired(String runId, String conversationId,
                                             String approvalId, Integer studentId, Integer score,
                                             String summary) {
        return approvalRequired(runId, conversationId, approvalId, studentId, score, "updateViolationScore", summary);
    }

    public static AgentEvent approvalRequired(String runId, String conversationId,
                                             String approvalId, Integer studentId, Integer score,
                                             String toolName, String summary) {
        return new AgentEvent(AgentEventType.APPROVAL_REQUIRED, runId, conversationId, summary,
                System.currentTimeMillis(), approvalId, studentId, score, toolName);
    }
}
