package com.xhz.ai.service;

import com.xhz.ai.dto.PendingApproval;
import com.xhz.pojo.param.EmpAddParam;

import java.util.List;

/**
 * 统一 HITL：写操作只建确认单，确认后按 {@link com.xhz.ai.dto.ApprovalType} 分发到业务 Service。
 */
public interface ApprovalService {

    PendingApproval createViolationScore(String runId, String conversationId, Integer studentId, Integer score);

    PendingApproval createEmpSave(String runId, String conversationId, EmpAddParam param);

    PendingApproval createEmpDelete(String runId, String conversationId, List<Integer> empIds);

    PendingApproval createDeptSave(String runId, String conversationId, String deptName);

    PendingApproval createDeptUpdate(String runId, String conversationId, Integer deptId, String deptName);

    PendingApproval createDeptDelete(String runId, String conversationId, Integer deptId);

    PendingApproval createSetAlarm(String runId, String conversationId, String alarmTime, String eventDescription);

    String approve(String approvalId);

    void reject(String approvalId);
}
