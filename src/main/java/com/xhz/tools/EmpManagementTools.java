package com.xhz.tools;

import com.xhz.pojo.Emp;
import com.xhz.pojo.param.EmpAddParam;
import com.xhz.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 员工管理 — AI 工具适配层（Spring AI 2.0.0）
 *
 * 职责：参数兜底校验 → 委托 EmpService → 拼自然语言战报
 * 严禁：直接注入 Mapper、编写实体组装逻辑、手动管理事务
 */
@Slf4j
@Component("empManagementTools")
public class EmpManagementTools {

    private final EmpService empService;

    public EmpManagementTools(EmpService empService) {
        this.empService = empService;
    }

    /**
     * 新员工入职登记
     *
     * 保存员工基本信息及多段工作经历。当用户说「入职」「新员工登记」「添加员工」时调用。
     */
    @Tool(description = "人事管理工具：用于新员工入职登记。保存员工基本信息及多段工作经历。如果用户说「入职」「新员工登记」「添加员工」时调用。")
    public String save(EmpAddParam param) {

        // ① 参数兜底：大模型可能漏参，提前拦截并给出纠偏指令
        if (param == null || param.username() == null || param.name() == null || param.phone() == null) {
            return "操作被拒绝：未收到有效的员工核心信息，请让用户务必提供用户名、姓名、性别和手机号。";
        }

        try {
            // ② 委托 Service 层处理核心业务（事务由 Service 的 @Transactional 管理）
            Emp emp = empService.addEmp(param);

            // ③ 拼接自然语言战报返回给大模型
            int exprCount = (param.exprList() != null) ? param.exprList().size() : 0;
            return String.format("员工保存成功！系统已生成员工ID：%d，姓名：%s，并成功登记了 %d 条历史工作经历。",
                    emp.getId(), emp.getName(), exprCount);

        } catch (Exception e) {
            // ④ 记录真实异常到后台日志，向大模型返回友好自然语言提示
            log.error("AI 自动保存员工失败，入参 param: {}", param, e);
            return "保存员工时发生异常，操作已自动回滚。错误详情：" + e.getMessage();
        }
    }

    /**
     * 批量删除员工
     *
     * 根据员工 ID 列表批量删除员工基本信息及其关联的历史工作经历。
     * 当用户要求「开除」「删除员工」时调用。
     */
    @Tool(description = "人事管理工具：用于根据员工ID或ID列表，批量删除员工基本信息及其关联的历史工作经历。当用户要求开除、删除员工时调用。")
    public String deleteByIds(
            @ToolParam(description = "要删除的员工ID列表，必须是数字数组，例如 [1, 2, 3]") List<Integer> ids) {

        // ① 参数兜底
        if (ids == null || ids.isEmpty()) {
            return "操作被拒绝：未提供有效的员工ID列表，请让用户明确指定要删除的员工。";
        }

        try {
            // ② 委托 Service 层处理
            int empCount = empService.deleteEmpByIds(ids);

            // ③ 拼接自然语言战报
            return String.format("员工删除成功！成功删除了 %d 名员工及其关联的全部历史工作经历。", empCount);

        } catch (Exception e) {
            // ④ 记录真实异常到后台日志，向大模型返回友好自然语言提示
            log.error("AI 自动批量删除员工失败, 入参 ids: {}", ids, e);
            return "数据库执行删除时发生异常，操作已自动回滚。错误详情：" + e.getMessage();
        }
    }
}
