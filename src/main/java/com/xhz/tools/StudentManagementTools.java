package com.xhz.tools;

import com.xhz.pojo.Student;
import com.xhz.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * 学员教务管理 — AI 工具适配层（Spring AI 2.0.0）
 *
 * 职责：参数兜底校验 → 委托 StudentService → 拼自然语言战报
 * 严禁：直接注入 Mapper、编写业务逻辑、手动管理事务
 */
@Slf4j
@Component("studentManagementTools")
public class StudentManagementTools {

    private final StudentService studentService;

    public StudentManagementTools(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * 违纪扣分请求参数
     *
     * 配合 @JsonPropertyDescription，Spring AI 会自动解析为
     * 大模型能理解的 JSON Schema 说明书
     */
    public record ViolationRequest(
            @JsonPropertyDescription("学员的ID或学号（数字类型），例如 1、5")
            Integer studentId,

            @JsonPropertyDescription("需要增加的违纪扣分数值，正整数，例如 2、5、10")
            Integer score
    ) {
    }

    /**
     * 更新学员违纪扣分
     *
     * 当用户要求给某个学员扣分、记录违纪、增加违纪分数时，大模型必须调用此工具。
     */
    @Tool(description = "根据学员的ID或学号，记录违纪并扣除相应的分数。当用户要求给某个学员扣分、记录违纪、增加违纪分数时，必须调用此工具。调用后将返回扣分是否成功。")
    public String updateViolationScore(ViolationRequest request) {

        Integer studentId = request.studentId();
        Integer score = request.score();

        // ① 参数兜底：大模型可能漏参，提前拦截并给出纠偏指令
        if (studentId == null || studentId <= 0) {
            return "操作失败：大模型未提取到有效的学员 ID。";
        }
        if (score == null || score <= 0) {
            score = 2; // 大模型未明确扣分值时，默认扣 2 分
        }

        try {
            // ② 委托 Service 层处理核心业务（事务由 Service 的 @Transactional 管理）
            Student updated = studentService.addViolationScore(studentId, score);

            // ③ 拼接自然语言战报返回给大模型
            return String.format("扣分成功！学员【%s】当前累计违纪 %d 次，总违纪扣分 %d 分。",
                    updated.getName(),
                    updated.getViolationCount(),
                    updated.getViolationScore());

        } catch (Exception e) {
            // ④ 记录真实异常到后台日志，向大模型返回友好自然语言提示
            log.error("AI 自动违纪扣分失败, studentId: {}, score: {}", studentId, score, e);
            return "扣分操作失败：" + e.getMessage();
        }
    }
}
