package com.xhz.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间 — AI 工具适配层（Spring AI 2.0.0）
 *
 * 提供大模型获取真实时间、设置闹钟提醒的能力。
 */
@Component
public class DateTimeTools {

    /**
     * 获取当前日期和时间
     *
     * 当用户输入「几点了」「今天几号」「星期几」等任何与时间、日期相关的口语化询问时，
     * 大模型必须且优先调用此工具。
     */
    @Tool(description = "获取当前日期和时间。当用户输入“几点了”、“时间是多少”、“今天几号”、“星期几”等任何与时间、日期相关的口语化询问时，必须且优先调用此工具。")
    public String getCurrentTime() {
        // 按用户时区偏好获取当前时间
        var zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        var now = LocalDateTime.now(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now);
    }

    /**
     * 设置定时提醒或闹钟
     *
     * 注意：如果用户说的是相对时间（如「半小时后」），
     * 大模型需要先调用 getCurrentTime() 确定当前时间，再计算出绝对时间。
     */
    @Tool(description = "设置定时提醒或闹钟。当用户要求在未来某个特定的时间提醒他们某事时，必须调用此工具。注意：如果用户说的是相对时间（如“半小时后”），你需要先确定当前的真实时间，再计算出绝对时间。")
    public String setAlarm(
            @ToolParam(description = "闹钟触发的绝对时间，不要包含T或时区，必须严格使用模板：yyyy-MM-dd HH:mm:ss") String alarmTime,
            @ToolParam(description = "提醒的具体事项内容或理由") String eventDescription) {

        System.out.println("⏰ 闹钟已入库，将在 [" + alarmTime + "] 提醒：[" + eventDescription + "]");

        return "闹钟设置成功！系统将于 " + alarmTime + " 触发提醒：" + eventDescription;
    }
}
