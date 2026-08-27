package com.xhz.ai.dto;

/**
 * 班主任 Agent 一次运行过程中推给前端的事件类型。
 */
public enum AgentEventType {

    /** 一次 Agent 运行开始 */
    RUN_STARTED,

    /** 大模型流式输出的一小段文本 */
    DELTA,

    /** 开始调用某个 Tool */
    TOOL_START,

    /** Tool 执行结束（含返回摘要） */
    TOOL_RESULT,

    /** 违纪证据图视觉识别结果（尚未写库） */
    VISION_RESULT,

    /** 写操作等待班主任确认（HITL） */
    APPROVAL_REQUIRED,

    /** 本次运行正常结束 */
    RUN_FINISHED,

    /** 本次运行失败 */
    ERROR
}
