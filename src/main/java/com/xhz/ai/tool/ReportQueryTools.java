package com.xhz.ai.tool;

import com.xhz.pojo.JobOption;
import com.xhz.pojo.StuOption;
import com.xhz.service.ReportService;
import com.xhz.anno.RequirePermission;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 报表只读查询，复用 ReportService。
 */
@Component("reportQueryTools")
public class ReportQueryTools {

    private final ReportService reportService;
    private final QueryToolSupport queryToolSupport;

    public ReportQueryTools(ReportService reportService, QueryToolSupport queryToolSupport) {
        this.reportService = reportService;
        this.queryToolSupport = queryToolSupport;
    }

    @RequirePermission("report:query")
    @Tool(description = """
            查询教务统计报表。只读，不改库。
            type 只能是：EMP_JOB（各职位员工人数）、EMP_GENDER（员工性别人数）、\
            STUDENT_DEGREE（学员学历分布）、STUDENT_COUNT（各班学员人数）。
            用户问有多少老师、男女比例、本科学员多少、各班人数时必须调用。
            """)
    public String queryReport(@ToolParam(description = "报表类型：EMP_JOB / EMP_GENDER / STUDENT_DEGREE / STUDENT_COUNT") String type,
                              ToolContext toolContext) {
        String kind = type == null ? "" : type.trim().toUpperCase().replace('-', '_');
        return queryToolSupport.run(toolContext, "queryReport", "报表 " + kind, () -> switch (kind) {
            case "EMP_JOB" -> {
                JobOption option = reportService.getEmpJobData();
                yield "各职位员工人数：" + QueryToolTexts.pairList(option.getJobList(), option.getDataList());
            }
            case "EMP_GENDER" -> {
                List<Map> rows = reportService.getEmpGenderData();
                yield "员工性别分布：" + QueryToolTexts.namedValueMaps(rows);
            }
            case "STUDENT_DEGREE" -> {
                List<Map> rows = reportService.getStudentDegreeData();
                yield "学员学历分布：" + QueryToolTexts.namedValueMaps(rows);
            }
            case "STUDENT_COUNT" -> {
                StuOption option = reportService.getStudentCountData();
                yield "各班学员人数：" + QueryToolTexts.pairList(option.getClazzList(), option.getDataList());
            }
            default -> "不支持的报表类型。请使用 EMP_JOB、EMP_GENDER、STUDENT_DEGREE 或 STUDENT_COUNT。";
        });
    }
}
