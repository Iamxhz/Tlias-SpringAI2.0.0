package com.xhz.pojo;

import lombok.Data;

/**
 * AI 聊天请求 DTO
 * 前端 ai-chat.html 的 POST /ai/chat 请求体
 * 业务上下文由 @Tool Function Calling 自动从自然语言中提取，已废弃手动 studentId 字段
 */
@Data
public class ChatRequest {
    /** 用户输入的消息 */
    private String message;
}
