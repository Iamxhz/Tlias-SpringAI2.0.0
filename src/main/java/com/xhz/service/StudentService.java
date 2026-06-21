package com.xhz.service;

import com.xhz.pojo.PageResult;
import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;

import java.util.List;

public interface StudentService {
    PageResult<Student> getStuPage(StudentQueryParam queryParam);

    void deleteById(List<Integer> ids);

    void save(Student student);

    Student getStuById(Integer id);

    void updateById(Student student);

    void updateViolation(Integer id, Short score);

    /** 根据学号查询学员 */
    Student getStuByNo(String no);

    /** 根据姓名模糊查询学员列表 */
    List<Student> getStuByName(String name);

    /**
     * AI 工具调用：原子增加违纪扣分
     *
     * @param id    学员 ID
     * @param score 需增加的扣分值
     * @return 更新后的学员实体（含最新 violationCount / violationScore）
     * @throws RuntimeException 学员不存在或数据库异常（由 Tool 层 catch 后转自然语言）
     */
    Student addViolationScore(Integer id, Integer score);
}
