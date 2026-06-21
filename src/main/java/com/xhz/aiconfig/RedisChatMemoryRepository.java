package com.xhz.aiconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于 Redis 的 ChatMemory 存储引擎（StringRedisTemplate 方案）
 *
 * Redis 结构：
 * <pre>
 *   tlias:chat:memory:{conversationId}  →  JSON 数组（消息列表）
 *   tlias:chat:memory:index             →  Redis Set（所有会话 ID）
 * </pre>
 *
 * StringRedisTemplate 由 spring-boot-starter-data-redis 自动配置，
 * Key/Value 均为 String，可在 Redis CLI 直接 GET 查看。
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "tlias:chat:memory:";
    private static final String INDEX_KEY = "tlias:chat:memory:index";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate redis) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    @NotNull
    @Override
    public List<String> findConversationIds() {
        Set<String> ids = redis.opsForSet().members(INDEX_KEY);
        return ids != null ? new ArrayList<>(ids) : Collections.emptyList();
    }

    @NotNull
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = redis.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null) {
            return Collections.emptyList();
        }
        return deserialize(json);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String json = serialize(messages);
        redis.opsForValue().set(KEY_PREFIX + conversationId, json);
        redis.opsForSet().add(INDEX_KEY, conversationId);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redis.delete(KEY_PREFIX + conversationId);
        redis.opsForSet().remove(INDEX_KEY, conversationId);
    }

    // ==================== JSON 序列化 ====================

    private String serialize(List<Message> messages) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("mType", msg.getMessageType().name());
            map.put("content", msg.getText());
            list.add(map);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    private List<Message> deserialize(String json) {
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            List<Message> messages = new ArrayList<>();
            for (Map<String, Object> map : list) {
                String mType = (String) map.get("mType");
                String content = (String) map.getOrDefault("content", "");
                messages.add(switch (MessageType.valueOf(mType)) {
                    case USER      -> new UserMessage(content);
                    case SYSTEM    -> new SystemMessage(content);
                    case ASSISTANT -> new AssistantMessage(content);
                    case TOOL      -> ToolResponseMessage.builder().build();
                });
            }
            return messages;
        } catch (Exception e) {
            throw new RuntimeException("反序列化消息失败", e);
        }
    }
}
