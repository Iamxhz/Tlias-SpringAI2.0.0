package com.xhz.ai.runtime;

import com.xhz.ai.dto.AgentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 conversationId 登记旁路事件通道，供 Tool 推送 APPROVAL_REQUIRED。
 */
@Component
public class AgentRunRegistry {

    private final ConcurrentHashMap<String, RunHandle> active = new ConcurrentHashMap<>();

    public void register(String runId, String conversationId, Sinks.Many<AgentEvent> sideEvents) {
        active.put(conversationId, new RunHandle(runId, conversationId, sideEvents));
    }

    public void unregister(String conversationId) {
        active.remove(conversationId);
    }

    public RunHandle get(String conversationId) {
        return active.get(conversationId);
    }

    public record RunHandle(
            String runId,
            String conversationId,
            Sinks.Many<AgentEvent> sideEvents
    ) {
        public void emit(AgentEvent event) {
            sideEvents.tryEmitNext(event);
        }
    }
}
