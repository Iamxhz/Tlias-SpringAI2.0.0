package com.xhz.ai.tool;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.service.ClazzService;
import com.xhz.anno.RequirePermission;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 班级只读查询。增删改仍走 REST，不在 Agent 写库。
 */
@Component("clazzQueryTools")
public class ClazzQueryTools {

    private final ClazzService clazzService;
    private final QueryToolSupport queryToolSupport;

    public ClazzQueryTools(ClazzService clazzService, QueryToolSupport queryToolSupport) {
        this.clazzService = clazzService;
        this.queryToolSupport = queryToolSupport;
    }

    @RequirePermission("clazz:query")
    @Tool(description = "查询全部班级列表。用户问有哪些班级、班级名单、开了哪些班时调用。只读，不改库。")
    public String listClazzs(ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "listClazzs", "查询班级列表", () -> {
            List<Clazz> list = clazzService.getClazzs();
            if (list == null || list.isEmpty()) {
                return "当前没有班级数据。";
            }
            return "共" + list.size() + "个班级：" + list.stream()
                    .map(QueryToolTexts::formatClazz)
                    .reduce((a, b) -> a + "；" + b)
                    .orElse("");
        });
    }

    @RequirePermission("clazz:query")
    @Tool(description = "按班级名称搜索班级。用户提到某期班名时调用。只读。")
    public String searchClazzs(@ToolParam(description = "班级名称，可模糊，例如 Java") String name,
                               ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "searchClazzs", "搜索班级 name=" + name, () -> {
            ClazzQueryParam param = new ClazzQueryParam();
            param.setPage(1);
            param.setPageSize(10);
            if (name != null && !name.isBlank()) {
                param.setName(name.trim());
            }
            PageResult page = clazzService.getClazzsPage(param);
            List<Clazz> rows = page == null ? List.of() : page.getRows();
            if (rows == null || rows.isEmpty()) {
                return "没有匹配的班级。";
            }
            return "共" + page.getTotal() + "个班级：" + rows.stream()
                    .map(QueryToolTexts::formatClazz)
                    .reduce((a, b) -> a + "；" + b)
                    .orElse("");
        });
    }

    @RequirePermission("clazz:query")
    @Tool(description = "按班级ID查询详情。已知班级ID时调用。只读。")
    public String getClazzById(@ToolParam(description = "班级ID") Integer id, ToolContext toolContext) {
        return queryToolSupport.run(toolContext, "getClazzById", "班级ID=" + id, () -> {
            if (id == null) {
                return "请提供班级ID。";
            }
            return QueryToolTexts.formatClazz(clazzService.getClazzById(id));
        });
    }
}
