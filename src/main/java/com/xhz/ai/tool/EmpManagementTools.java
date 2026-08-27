package com.xhz.ai.tool;

import com.xhz.ai.runtime.AgentToolEventEmitter;
import com.xhz.ai.runtime.HitlSupport;
import com.xhz.ai.service.ApprovalService;
import com.xhz.anno.RequirePermission;
import com.xhz.pojo.Emp;
import com.xhz.pojo.EmpQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.param.EmpAddParam;
import com.xhz.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 员工管理 AI Tool：写操作只生成待确认指令。
 */
@Slf4j
@Component("empManagementTools")
public class EmpManagementTools {

    private static final String SAVE_TOOL = "saveEmp";
    private static final String DELETE_TOOL = "deleteEmpByIds";

    private final ApprovalService approvalService;
    private final AgentToolEventEmitter toolEventEmitter;
    private final HitlSupport hitlSupport;
    private final EmpService empService;
    private final QueryToolSupport queryToolSupport;

    public EmpManagementTools(ApprovalService approvalService,
                              AgentToolEventEmitter toolEventEmitter,
                              HitlSupport hitlSupport,
                              EmpService empService,
                              QueryToolSupport queryToolSupport) {
        this.approvalService = approvalService;
        this.toolEventEmitter = toolEventEmitter;
        this.hitlSupport = hitlSupport;
        this.empService = empService;
        this.queryToolSupport = queryToolSupport;
    }

    @RequirePermission("emp:query")
    @Tool(description = "查询员工列表。用户问有哪些员工、员工名单时调用。只读，不改库。")
    public String listEmps(ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "listEmps", "查询员工列表", () -> {
            EmpQueryParam param = new EmpQueryParam();
            param.setPage(1);
            param.setPageSize(20);
            PageResult page = empService.page(param);
            List<Emp> rows = page == null ? List.of() : page.getRows();
            if (rows == null || rows.isEmpty()) {
                return "当前没有员工数据。";
            }
            String body = rows.stream().map(QueryToolTexts::formatEmp).reduce((a, b) -> a + "；" + b).orElse("");
            return "共" + page.getTotal() + "人：" + body;
        });
    }

    @RequirePermission("emp:query")
    @Tool(description = "按姓名或性别搜索员工。用户提到某位老师/员工姓名、查男员工女员工时调用。只读。性别：男传1，女传2，不限则留空。")
    public String searchEmps(@ToolParam(description = "员工姓名，可模糊，例如 张三。不限则留空") String name,
                             @ToolParam(description = "性别，男=1，女=2，不限则留空") Integer gender,
                             ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "searchEmps", "搜索员工 name=" + name, () -> {
            EmpQueryParam param = new EmpQueryParam();
            param.setPage(1);
            param.setPageSize(10);
            if (name != null && !name.isBlank()) {
                param.setName(name.trim());
            }
            param.setGender(gender);
            PageResult page = empService.page(param);
            List<Emp> rows = page == null ? List.of() : page.getRows();
            if (rows == null || rows.isEmpty()) {
                return "没有匹配的员工。";
            }
            String body = rows.stream().map(QueryToolTexts::formatEmp).reduce((a, b) -> a + "；" + b).orElse("");
            return "共" + page.getTotal() + "人：" + body;
        });
    }

    @RequirePermission("emp:query")
    @Tool(description = "按员工ID查询详情。已知员工ID、要看职位或部门时调用。只读。")
    public String getEmpById(@ToolParam(description = "员工ID") Integer id, ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "getEmpById", "员工ID=" + id, () -> {
            if (id == null) {
                return "请提供员工ID。";
            }
            return QueryToolTexts.formatEmp(empService.getInfo(id));
        });
    }

    @RequirePermission("emp:save")
    @Tool(description = "人事管理：新员工入职登记。不会立刻写库，需班主任在界面确认后才保存员工及工作经历。用户说「入职」「新员工登记」「添加员工」时调用。")
    public String save(EmpAddParam param, ToolContext toolContext) {
        if (param == null || param.username() == null || param.name() == null || param.phone() == null) {
            String msg = "操作被拒绝：缺少用户名、姓名或手机号。";
            toolEventEmitter.emitStart(toolContext, SAVE_TOOL, "param=invalid");
            toolEventEmitter.emitResult(toolContext, SAVE_TOOL, msg, false);
            return msg;
        }
        String summary = "入职登记 name=" + param.name() + ", username=" + param.username();
        return hitlSupport.submit(toolContext, SAVE_TOOL, summary,
                (runId, conversationId) -> approvalService.createEmpSave(runId, conversationId, param));
    }

    @RequirePermission("emp:delete")
    @Tool(description = "人事管理：按员工ID批量删除。不会立刻删库，需班主任确认。用户要求开除、删除员工时必须调用。")
    public String deleteByIds(
            @ToolParam(description = "要删除的员工ID列表，必须是数字数组，例如 [1, 2, 3]") List<Integer> ids,
            ToolContext toolContext) {

        if (ids == null || ids.isEmpty()) {
            String msg = "操作被拒绝：未提供有效的员工ID列表。";
            toolEventEmitter.emitStart(toolContext, DELETE_TOOL, "ids=empty");
            toolEventEmitter.emitResult(toolContext, DELETE_TOOL, msg, false);
            return msg;
        }
        return hitlSupport.submit(toolContext, DELETE_TOOL, "ids=" + ids,
                (runId, conversationId) -> approvalService.createEmpDelete(runId, conversationId, ids));
    }
}
