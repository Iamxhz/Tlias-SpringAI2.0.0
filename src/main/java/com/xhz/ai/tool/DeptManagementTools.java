package com.xhz.ai.tool;

import com.xhz.ai.runtime.AgentToolEventEmitter;
import com.xhz.ai.runtime.HitlSupport;
import com.xhz.ai.service.ApprovalService;
import com.xhz.anno.RequirePermission;
import com.xhz.pojo.Dept;
import com.xhz.service.DeptService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理 AI Tool：查询直读，增删改走 HITL。
 */
@Component("deptManagementTools")
public class DeptManagementTools {

    private final DeptService deptService;
    private final ApprovalService approvalService;
    private final AgentToolEventEmitter toolEventEmitter;
    private final HitlSupport hitlSupport;

    public DeptManagementTools(DeptService deptService,
                               ApprovalService approvalService,
                               AgentToolEventEmitter toolEventEmitter,
                               HitlSupport hitlSupport) {
        this.deptService = deptService;
        this.approvalService = approvalService;
        this.toolEventEmitter = toolEventEmitter;
        this.hitlSupport = hitlSupport;
    }

    @RequirePermission("dept:query")
    @Tool(description = "查询全部部门列表。用户询问有哪些部门、部门名单时调用。")
    public String listDepts(ToolContext toolContext) {
        toolEventEmitter.emitStart(toolContext, "listDepts", "查询部门列表");
        List<Dept> depts = deptService.findAll();
        if (depts == null || depts.isEmpty()) {
            String msg = "当前没有部门数据。";
            toolEventEmitter.emitResult(toolContext, "listDepts", msg, true);
            return msg;
        }
        String result = depts.stream()
                .map(d -> "ID=" + d.getId() + "，名称=" + d.getName())
                .collect(Collectors.joining("；"));
        toolEventEmitter.emitResult(toolContext, "listDepts", result, true);
        return "部门列表：" + result;
    }

    @RequirePermission("dept:save")
    @Tool(description = "新增部门。不会立刻写库，需班主任确认。用户说新建部门、添加部门时调用。")
    public String saveDept(@ToolParam(description = "部门名称，例如 教研部") String name,
                           ToolContext toolContext) {
        if (name == null || name.isBlank()) {
            return reject("saveDept", "部门名称不能为空。", toolContext);
        }
        return hitlSupport.submit(toolContext, "saveDept", "新增部门 name=" + name.trim(),
                (runId, conversationId) -> approvalService.createDeptSave(runId, conversationId, name.trim()));
    }

    @RequirePermission("dept:update")
    @Tool(description = "修改部门名称。不会立刻写库，需班主任确认。")
    public String updateDept(@ToolParam(description = "部门ID") Integer id,
                             @ToolParam(description = "新的部门名称") String name,
                             ToolContext toolContext) {
        if (id == null || name == null || name.isBlank()) {
            return reject("updateDept", "部门ID和名称都不能为空。", toolContext);
        }
        return hitlSupport.submit(toolContext, "updateDept", "修改部门 id=" + id + ", name=" + name.trim(),
                (runId, conversationId) ->
                        approvalService.createDeptUpdate(runId, conversationId, id, name.trim()));
    }

    @RequirePermission("dept:delete")
    @Tool(description = "按ID删除部门。不会立刻删库，需班主任确认。部门下有员工时确认后仍会失败。")
    public String deleteDept(@ToolParam(description = "要删除的部门ID") Integer id,
                             ToolContext toolContext) {
        if (id == null) {
            return reject("deleteDept", "部门ID不能为空。", toolContext);
        }
        return hitlSupport.submit(toolContext, "deleteDept", "删除部门 id=" + id,
                (runId, conversationId) -> approvalService.createDeptDelete(runId, conversationId, id));
    }

    private String reject(String toolName, String msg, ToolContext toolContext) {
        toolEventEmitter.emitStart(toolContext, toolName, "参数无效");
        toolEventEmitter.emitResult(toolContext, toolName, msg, false);
        return msg;
    }
}
