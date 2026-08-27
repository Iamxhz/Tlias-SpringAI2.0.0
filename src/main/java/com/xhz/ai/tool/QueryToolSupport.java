package com.xhz.ai.tool;

import com.xhz.ai.runtime.AgentToolEventEmitter;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 只读查询 Tool 共用：发 TOOL_START/RESULT，异常转成自然语言。
 */
@Component
public class QueryToolSupport {

    private final AgentToolEventEmitter toolEventEmitter;

    public QueryToolSupport(AgentToolEventEmitter toolEventEmitter) {
        this.toolEventEmitter = toolEventEmitter;
    }

    public String run(ToolContext toolContext, String toolName, String summary, Supplier<String> action) {
        toolEventEmitter.emitStart(toolContext, toolName, summary);
        try {
            String result = action.get();
            if (result == null || result.isBlank()) {
                result = "没有查到数据。";
            }
            toolEventEmitter.emitResult(toolContext, toolName, result, true);
            return result;
        } catch (Exception e) {
            String msg = "查询失败：" + e.getMessage();
            toolEventEmitter.emitResult(toolContext, toolName, msg, false);
            return msg;
        }
    }
}
