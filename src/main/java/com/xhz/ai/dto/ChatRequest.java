package com.xhz.ai.dto;

import lombok.Data;

/**
 * 班主任 Agent 聊天请求
 */
@Data
public class ChatRequest {

    /** 用户输入的消息 */
    private String message;

    /**
     * 会话 ID。同一会话多轮对话应传同一个值，才能命中 Redis Chat Memory。
     * 为空时由 Runtime 自动生成，并在 RUN_STARTED 事件中回传给前端。
     */
    private String conversationId;

    /**
     * 违纪证据图 URL（先走 {@code POST /upload}）。有图时由视觉模型抽学员/情节，再走 Tool + HITL。
     */
    private String imageUrl;

    /**
     * 证据图 Base64（可带 data URL 前缀）。OSS 私有读时优先用这个，避免服务端下载失败。
     */
    private String imageBase64;
}
