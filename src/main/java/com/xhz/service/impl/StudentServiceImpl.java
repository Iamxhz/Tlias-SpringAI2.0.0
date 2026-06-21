package com.xhz.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xhz.mapper.StudentMapper;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;
import com.xhz.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员管理 — Service 层
 * <p>
 * 职责：核心业务逻辑、Mapper 操作、事务管理。
 * 返回纯数据（实体/影响行数），绝不返回大模型自然语言战报。
 */
@Slf4j
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public PageResult<Student> getStuPage(StudentQueryParam queryParam) {
        PageHelper.startPage(queryParam.getPage(), queryParam.getPageSize());
        List<Student> students = studentMapper.getStuPage(queryParam);
        Page<Student> p = (Page<Student>) students;
        return new PageResult<>(p.getTotal(), p.getResult());
    }

    @Override
    public void deleteById(List<Integer> ids) {
        studentMapper.deleteByIds(ids);
    }

    @Override
    public void save(Student student) {
        student.setCreateTime(LocalDateTime.now());
        student.setUpdateTime(LocalDateTime.now());
        student.setViolationCount((short) 0);
        student.setViolationScore((short) 0);
        studentMapper.save(student);
    }

    @Override
    public Student getStuById(Integer id) {
        return studentMapper.getStuById(id);
    }

    @Override
    public void updateById(Student student) {
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.updateById(student);
    }

    @Override
    public void updateViolation(Integer id, Short score) {
        Student student = studentMapper.getStuById(id);
        student.setViolationScore((short) (student.getViolationScore() + score));
        student.setViolationCount((short) (student.getViolationCount() + 1));
        studentMapper.updateById(student);
    }

    @Override
    public Student getStuByNo(String no) {
        return studentMapper.getStuByNo(no);
    }

    @Override
    public List<Student> getStuByName(String name) {
        return studentMapper.getStuByName(name);
    }

    // ==================== AI Tool Calling 专用方法 ====================

    /**
     * AI 工具调用：原子增加违纪扣分
     * <p>
     * 完整链路：学员存在性校验 → 原子更新扣分 → 重查最新数据 → 返回实体
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Student addViolationScore(Integer id, Integer score) {
        // 1. 学员存在性校验 — 不通过直接抛异常，由 Tool 层 catch 转友好提示
        Student student = studentMapper.getStuById(id);
        if (student == null) {
            throw new RuntimeException("未找到 ID 为 " + id + " 的学员。");
        }

        // 2. 原子更新违纪扣分（避免并发覆盖）
        studentMapper.addViolationScore(id, score);

        // 3. 重查获取最新数据，返回完整的学员实体供 Tool 层拼战报
        Student updated = studentMapper.getStuById(id);
        log.info("AI 违纪扣分成功，学员ID：{}，姓名：{}，当前累计扣分：{}，违纪次数：{}",
                id, updated.getName(), updated.getViolationScore(), updated.getViolationCount());

        return updated;
    }
}
