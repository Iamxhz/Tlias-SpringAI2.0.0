package com.xhz.service;

import com.xhz.pojo.Emp;
import com.xhz.pojo.EmpQueryParam;
import com.xhz.pojo.LoginInfo;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.param.EmpAddParam;

import java.util.List;

public interface EmpService {
    /**
     * 分页查询
     */
    PageResult page(EmpQueryParam empQueryParam);

    /**
     * Web 表单新增员工
     */
    void save(Emp emp);

    Emp getInfo(Integer id);

    void update(Emp emp);

    List<Emp> getEmps();

    LoginInfo login(Emp emp);

    // ==================== AI Tool Calling 专用方法 ====================

    /**
     * AI 工具调用：新员工入职登记
     * 含 EmpAddParam → Emp 实体组装、密码/时间补全、工作经历转换、事务保存
     *
     * @param param 大模型解析出的员工入职参数
     * @return 保存后的 Emp 实体（含自动回填的 ID 和 exprList）
     * @throws RuntimeException 参数校验失败或数据库异常（由 Tool 层 catch 后转自然语言）
     */
    Emp addEmp(EmpAddParam param);

    /**
     * AI 工具调用：批量删除员工及关联工作经历
     *
     * @param ids 待删除的员工 ID 列表
     * @return 删除的员工数量
     * @throws RuntimeException 参数校验失败或数据库异常（由 Tool 层 catch 后转自然语言）
     */
    int deleteEmpByIds(List<Integer> ids);
}
