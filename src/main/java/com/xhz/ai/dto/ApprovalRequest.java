package com.xhz.ai.dto;

import lombok.Data;

/**
 * 班主任确认 / 拒绝 Agent 写操作
 */
@Data
public class ApprovalRequest {

    /** 待确认单号（APPROVAL_REQUIRED 事件里的 approvalId） */
    private String approvalId;

    /** true=确认执行，false=拒绝 */
    private Boolean approved;
}
