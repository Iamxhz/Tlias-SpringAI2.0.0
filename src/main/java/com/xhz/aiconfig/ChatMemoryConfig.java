package com.xhz.aiconfig;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置 — Spring AI 2.0.0
 *
 * 存储引擎：RedisChatMemoryRepository（StringRedisTemplate → Redis 持久化）
 * 窗口大小：最近 20 条消息，防止上下文溢出
 * 会话隔离：由 MessageChatMemoryAdvisor 在请求级通过 conversationId 区分
 */
@Configuration
public class ChatMemoryConfig {

    private final RedisChatMemoryRepository redisRepo;

    public ChatMemoryConfig(RedisChatMemoryRepository redisRepo) {
        this.redisRepo = redisRepo;
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisRepo)
                .maxMessages(20)
                .build();
    }
}
