package com.xhz.ai.dto;

import java.time.Instant;
import java.util.List;

/**
 * 统一待确认单。类型相关数据放在专用字段或 {@code payloadJson}。
 */
public record PendingApproval(
        String approvalId,
        ApprovalType type,
        String runId,
        String conversationId,
        Integer studentId,
        Integer score,
        List<Integer> empIds,
        String payloadJson,
        Instant createdAt
) {
}
