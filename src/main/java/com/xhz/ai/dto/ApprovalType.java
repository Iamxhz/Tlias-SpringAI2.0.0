package com.xhz.ai.dto;

/**
 * 待确认写操作类型。新增业务只需加枚举 + ApprovalService 分发分支。
 */
public enum ApprovalType {

    VIOLATION_SCORE,
    EMP_SAVE,
    EMP_DELETE,
    DEPT_SAVE,
    DEPT_UPDATE,
    DEPT_DELETE,
    SET_ALARM
}
