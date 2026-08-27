package com.xhz.ai.tool;

import com.xhz.ai.runtime.AgentToolEventEmitter;
import com.xhz.ai.runtime.HitlSupport;
import com.xhz.ai.service.ApprovalService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间 Tool：查时直读；设闹钟走 HITL。
 */
@Component
public class DateTimeTools {

    private final AgentToolEventEmitter toolEventEmitter;
    private final HitlSupport hitlSupport;
    private final ApprovalService approvalService;

    public DateTimeTools(AgentToolEventEmitter toolEventEmitter,
                         HitlSupport hitlSupport,
                         ApprovalService approvalService) {
        this.toolEventEmitter = toolEventEmitter;
        this.hitlSupport = hitlSupport;
        this.approvalService = approvalService;
    }

    @Tool(description = "获取当前日期和时间。用户问几点、今天几号、星期几时优先调用。")
    public String getCurrentTime(ToolContext toolContext) {
        String toolName = "getCurrentTime";
        toolEventEmitter.emitStart(toolContext, toolName, "查询当前系统时间");
        try {
            var zoneId = LocaleContextHolder.getTimeZone().toZoneId();
            var now = LocalDateTime.now(zoneId);
            String result = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now);
            toolEventEmitter.emitResult(toolContext, toolName, result, true);
            return result;
        } catch (Exception e) {
            String msg = "获取当前时间失败：" + e.getMessage();
            toolEventEmitter.emitResult(toolContext, toolName, msg, false);
            return msg;
        }
    }

    @Tool(description = "设置定时提醒或闹钟。不会立刻生效，需班主任确认。相对时间（如半小时后）须先换算成绝对时间 yyyy-MM-dd HH:mm:ss。")
    public String setAlarm(
            @ToolParam(description = "闹钟触发的绝对时间，不要包含T或时区，必须严格使用模板：yyyy-MM-dd HH:mm:ss") String alarmTime,
            @ToolParam(description = "提醒的具体事项内容或理由") String eventDescription,
            ToolContext toolContext) {

        if (alarmTime == null || alarmTime.isBlank() || eventDescription == null || eventDescription.isBlank()) {
            String msg = "操作被拒绝：闹钟时间和提醒事项都不能为空。";
            toolEventEmitter.emitStart(toolContext, "setAlarm", "参数无效");
            toolEventEmitter.emitResult(toolContext, "setAlarm", msg, false);
            return msg;
        }
        String summary = "设置闹钟 time=" + alarmTime + ", event=" + eventDescription;
        return hitlSupport.submit(toolContext, "setAlarm", summary,
                (runId, conversationId) ->
                        approvalService.createSetAlarm(runId, conversationId, alarmTime, eventDescription));
    }
}
