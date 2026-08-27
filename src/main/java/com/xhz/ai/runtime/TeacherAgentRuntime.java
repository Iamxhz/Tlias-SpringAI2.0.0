package com.xhz.ai.runtime;

import com.xhz.ai.dto.AgentEvent;
import com.xhz.ai.service.ViolationVisionService;
import com.xhz.utils.SecurityContextHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 班主任 Agent 运行编排：主路文本事件 + 旁路 HITL 事件 merge 后推 SSE。
 * 若带证据图，先走视觉识别，再把结构化结果交给 ChatClient（文本模型 + Tools + HITL）。
 */
@Component
public class TeacherAgentRuntime {

    public static final String CTX_RUN_ID = "runId";
    public static final String CTX_CONVERSATION_ID = "conversationId";
    /** 供 Tool 线程恢复权限上下文（SSE 可能不在请求线程执行） */
    public static final String CTX_SECURITY = "securityContext";

    private static final String DEFAULT_IMAGE_PROMPT = "请根据这张违纪证据图，按教务规章建议扣分并发起确认。";

    private final ChatClient agentChatClient;
    private final AgentRunRegistry agentRunRegistry;
    private final ViolationVisionService violationVisionService;

    public TeacherAgentRuntime(@Qualifier("agentChatClient") ChatClient agentChatClient,
                               AgentRunRegistry agentRunRegistry,
                               ViolationVisionService violationVisionService) {
        this.agentChatClient = agentChatClient;
        this.agentRunRegistry = agentRunRegistry;
        this.violationVisionService = violationVisionService;
    }

    public Flux<AgentEvent> run(String message, String conversationId) {
        return run(message, conversationId, null, null);
    }

    public Flux<AgentEvent> run(String message, String conversationId, String imageUrl, String imageBase64) {
        String runId = newRunId();
        String cid = normalizeConversationId(conversationId);
        boolean hasImage = StringUtils.hasText(imageUrl) || StringUtils.hasText(imageBase64);
        String text = message == null ? "" : message.trim();

        if (text.isEmpty() && !hasImage) {
            return Flux.just(AgentEvent.error(runId, cid, "请输入有效的消息内容，或上传违纪证据图。"));
        }

        Sinks.Many<AgentEvent> sideEvents = Sinks.many().multicast().onBackpressureBuffer();
        agentRunRegistry.register(runId, cid, sideEvents);
        Map<String, Object> toolCtx = buildToolContext(runId, cid);

        Flux<AgentEvent> started = Flux.just(AgentEvent.started(runId, cid));

        Mono<PreparedPrompt> prepared = hasImage
                ? Mono.fromCallable(() -> violationVisionService.analyzeEvidence(
                        StringUtils.hasText(imageUrl) ? imageUrl.trim() : null,
                        StringUtils.hasText(imageBase64) ? imageBase64.trim() : null))
                .subscribeOn(Schedulers.boundedElastic())
                .map(visionJson -> new PreparedPrompt(buildUserText(text, visionJson), visionJson, null))
                .onErrorResume(e -> Mono.just(new PreparedPrompt(
                        buildUserText(text.isEmpty() ? DEFAULT_IMAGE_PROMPT : text, null),
                        null,
                        "视觉识别失败：" + e.getMessage()
                )))
                : Mono.just(new PreparedPrompt(text, null, null));

        Flux<AgentEvent> rest = prepared.flatMapMany(p -> {
            Flux<AgentEvent> visionEvt;
            if (p.visionJson() != null) {
                visionEvt = Flux.just(AgentEvent.visionResult(runId, cid, brief(p.visionJson())));
            } else if (p.visionError() != null) {
                visionEvt = Flux.just(AgentEvent.visionResult(runId, cid, p.visionError()));
            } else {
                visionEvt = Flux.empty();
            }

            Flux<AgentEvent> deltas = agentChatClient.prompt()
                    .user(p.userText())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                    .toolContext(toolCtx)
                    .stream()
                    .content()
                    .map(chunk -> AgentEvent.delta(runId, cid, chunk));

            return visionEvt.concatWith(deltas).concatWith(Flux.just(AgentEvent.finished(runId, cid)));
        });

        Flux<AgentEvent> main = started
                .concatWith(rest)
                .doFinally(signal -> {
                    sideEvents.tryEmitComplete();
                    agentRunRegistry.unregister(cid);
                });

        return Flux.merge(main, sideEvents.asFlux())
                .onErrorResume(e -> Flux.just(
                        AgentEvent.error(runId, cid, "班主任 Agent 执行失败：" + e.getMessage())
                ));
    }

    private static String buildUserText(String userText, String visionJson) {
        if (visionJson == null || visionJson.isBlank()) {
            return userText == null || userText.isBlank() ? DEFAULT_IMAGE_PROMPT : userText;
        }
        String say = userText == null || userText.isBlank() ? DEFAULT_IMAGE_PROMPT : userText;
        return """
                用户上传了一张违纪证据图。下面是视觉模型的识别结果（仅供参考，不能直接当学员 ID，更不能声称已经扣分）：
                %s

                用户补充说明：%s

                请先调用 findStudents 用姓名或学号定位学员；能唯一确定后再按教务规章调用 updateViolationScore。
                重名、看不清或找不到时向班主任说明，不要编造 ID。
                """.formatted(visionJson, say);
    }

    private static String brief(String visionJson) {
        String compact = visionJson.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 400) {
            return compact;
        }
        return compact.substring(0, 400) + "…";
    }

    private static Map<String, Object> buildToolContext(String runId, String conversationId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put(CTX_RUN_ID, runId);
        ctx.put(CTX_CONVERSATION_ID, conversationId);
        SecurityContextHolder.SecurityContext security = SecurityContextHolder.get();
        if (security != null) {
            ctx.put(CTX_SECURITY, security);
        }
        return ctx;
    }

    private static String newRunId() {
        return "run-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "teacher-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        return conversationId.trim();
    }

    private record PreparedPrompt(String userText, String visionJson, String visionError) {
    }
}
