package com.xhz.ai.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.xhz.ai.runtime.AgentToolEventEmitter;
import com.xhz.ai.runtime.HitlSupport;
import com.xhz.ai.service.ApprovalService;
import com.xhz.anno.RequirePermission;
import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.Clazz;
import com.xhz.service.ClazzService;
import com.xhz.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学员 AI Tool：名单查询直读，违纪扣分只生成待确认指令。
 */
@Slf4j
@Component("studentManagementTools")
public class StudentManagementTools {

    private static final String TOOL_NAME = "updateViolationScore";

    private final ApprovalService approvalService;
    private final AgentToolEventEmitter toolEventEmitter;
    private final HitlSupport hitlSupport;
    private final StudentService studentService;
    private final ClazzService clazzService;
    private final QueryToolSupport queryToolSupport;

    public StudentManagementTools(ApprovalService approvalService,
                                  AgentToolEventEmitter toolEventEmitter,
                                  HitlSupport hitlSupport,
                                  StudentService studentService,
                                  ClazzService clazzService,
                                  QueryToolSupport queryToolSupport) {
        this.approvalService = approvalService;
        this.toolEventEmitter = toolEventEmitter;
        this.hitlSupport = hitlSupport;
        this.studentService = studentService;
        this.clazzService = clazzService;
        this.queryToolSupport = queryToolSupport;
    }

    public record ViolationRequest(
            @JsonPropertyDescription("学员的ID或学号（数字类型），例如 1、5")
            Integer studentId,

            @JsonPropertyDescription("需要增加的违纪扣分数值，正整数，例如 2、5、10")
            Integer score
    ) {
    }

    @RequirePermission("student:query")
    @Tool(description = "按姓名或学号查找学员，返回 id、学号、姓名、当前违纪分。看图或口述姓名后、扣分前必须先调用；不要编造学员 ID。只读。")
    public String findStudents(@ToolParam(description = "学员姓名或学号，例如 陈小明、2025001") String keyword,
                               ToolContext toolContext) {
        String tool = "findStudents";
        String query = keyword == null ? "" : keyword.trim();
        toolEventEmitter.emitStart(toolContext, tool, "查找学员：" + query);
        if (!StringUtils.hasText(query)) {
            String msg = "请提供学员姓名或学号。";
            toolEventEmitter.emitResult(toolContext, tool, msg, false);
            return msg;
        }

        Map<Integer, Student> found = new LinkedHashMap<>();
        Student byNo = studentService.getStuByNo(query);
        if (byNo != null && byNo.getId() != null) {
            found.put(byNo.getId(), byNo);
        }
        if (query.matches("\\d+")) {
            try {
                Student byId = studentService.getStuById(Integer.parseInt(query));
                if (byId != null && byId.getId() != null) {
                    found.put(byId.getId(), byId);
                }
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        List<Student> byName = studentService.getStuByName(query);
        if (byName != null) {
            for (Student s : byName) {
                if (s != null && s.getId() != null) {
                    found.put(s.getId(), s);
                }
            }
        }

        if (found.isEmpty()) {
            String msg = "未找到姓名或学号匹配「" + query + "」的学员，请向班主任确认后再扣分。";
            toolEventEmitter.emitResult(toolContext, tool, msg, true);
            return msg;
        }

        List<String> lines = new ArrayList<>();
        for (Student s : found.values()) {
            lines.add(String.format("ID=%d，学号=%s，姓名=%s，违纪次数=%s，违纪扣分=%s",
                    s.getId(),
                    s.getNo(),
                    s.getName(),
                    s.getViolationCount(),
                    s.getViolationScore()));
        }
        if (found.size() > 1) {
            lines.add("存在多名匹配，扣分前必须让班主任确认是哪一位，不要自行挑选。");
        }
        String result = String.join("；", lines);
        toolEventEmitter.emitResult(toolContext, tool, result, true);
        return result;
    }

    @RequirePermission("student:query")
    @Tool(description = "分页查询学员名单。用户问有哪些学员、某班有哪些人、按学历筛选时调用。只读。学历：1初中2高中3大专4本科5硕士6博士。班级可传ID或名称。")
    public String listStudents(@ToolParam(description = "学员姓名，可模糊，不限则留空") String name,
                               @ToolParam(description = "班级ID，不限则留空") Integer clazzId,
                               @ToolParam(description = "班级名称，例如 Java 就业 26 期。与班级ID二选一") String clazzName,
                               @ToolParam(description = "最高学历代码，不限则留空") Integer degree,
                               ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "listStudents", "查询学员", () -> {
            Integer resolvedClazzId = clazzId;
            if (resolvedClazzId == null && clazzName != null && !clazzName.isBlank()) {
                List<Clazz> clazzs = clazzService.getClazzs();
                String key = clazzName.trim();
                List<Clazz> matched = clazzs == null ? List.of() : clazzs.stream()
                        .filter(c -> c.getName() != null && c.getName().contains(key))
                        .toList();
                if (matched.isEmpty()) {
                    return "未找到名称包含「" + key + "」的班级，请先调用 listClazzs。";
                }
                if (matched.size() > 1) {
                    return "匹配到多个班级：" + matched.stream()
                            .map(c -> "ID=" + c.getId() + " " + c.getName())
                            .reduce((a, b) -> a + "；" + b).orElse("")
                            + "。请指定班级ID后再查学员。";
                }
                resolvedClazzId = matched.get(0).getId();
            }
            StudentQueryParam param = new StudentQueryParam();
            param.setPage(1);
            param.setPageSize(15);
            if (name != null && !name.isBlank()) {
                param.setName(name.trim());
            }
            param.setClazzId(resolvedClazzId);
            param.setDegree(degree);
            PageResult<Student> page = studentService.getStuPage(param);
            List<Student> rows = page == null ? List.of() : page.getRows();
            if (rows == null || rows.isEmpty()) {
                return "没有匹配的学员。";
            }
            String body = rows.stream().map(QueryToolTexts::formatStudent).reduce((a, b) -> a + "；" + b).orElse("");
            return "共" + page.getTotal() + "人：" + body;
        });
    }

    @RequirePermission("student:query")
    @Tool(description = "按学员ID查询详情。已知学员ID时调用。只读。")
    public String getStudentById(@ToolParam(description = "学员ID") Integer id, ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "getStudentById", "学员ID=" + id, () -> {
            if (id == null) {
                return "请提供学员ID。";
            }
            return QueryToolTexts.formatStudent(studentService.getStuById(id));
        });
    }

    @RequirePermission("student:violation")
    @Tool(description = "根据学员ID发起违纪扣分申请（不会立刻改库，需班主任确认）。用户要求扣分、记录违纪，或已根据证据图定位到学员时必须调用。扣分前若只有姓名没有 ID，先调用 findStudents。")
    public String updateViolationScore(ViolationRequest request, ToolContext toolContext) {
        Integer studentId = request.studentId();
        Integer score = request.score();

        if (studentId == null || studentId <= 0) {
            String msg = "操作失败：未提取到有效的学员 ID。";
            toolEventEmitter.emitStart(toolContext, TOOL_NAME, "参数无效");
            toolEventEmitter.emitResult(toolContext, TOOL_NAME, msg, false);
            return msg;
        }
        if (score == null || score <= 0) {
            score = 2;
        }

        Integer finalScore = score;
        String summary = String.format("申请给学员 ID=%d 扣 %d 分", studentId, finalScore);
        return hitlSupport.submit(toolContext, TOOL_NAME, summary,
                (runId, conversationId) ->
                        approvalService.createViolationScore(runId, conversationId, studentId, finalScore));
    }
}
