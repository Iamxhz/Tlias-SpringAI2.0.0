package com.xhz.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xhz.anno.LogOperation;
import com.xhz.exception.BusinessException;
import com.xhz.mapper.ClazzMapper;
import com.xhz.mapper.StudentMapper;
import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClazzServiceImpl implements ClazzService {
    @Autowired
    private ClazzMapper clazzMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Override
    public PageResult getClazzsPage(ClazzQueryParam clazzQueryParam) {
        // 1. 设置分页参数
        PageHelper.startPage(clazzQueryParam.getPage(), clazzQueryParam.getPageSize());

        // 2. 执行查询
        List<Clazz> clazzList = clazzMapper.getClazzsPage(clazzQueryParam);

        // 3. 补充班级状态逻辑 (核心补充部分)
        LocalDate today = LocalDate.now(); // 获取当前日期

        for (Clazz clazz : clazzList) {
            LocalDate beginDate = clazz.getBeginDate();
            LocalDate endDate = clazz.getEndDate();

            // 健壮性判断：确保时间字段不为空，防止 NullPointerException
            if (beginDate != null && endDate != null) {
                if (today.isBefore(beginDate)) {
                    clazz.setStatus("未开班");
                } else if (today.isAfter(endDate)) {
                    clazz.setStatus("已结课");
                } else {
                    // 既不早于开课，也不晚于结课，说明正在进行中
                    clazz.setStatus("在读");
                }
            } else {
                // 如果数据库里存在脏数据（没有时间），给一个默认保底状态
                clazz.setStatus("未知");
            }
        }

        // 4. 封装并返回 PageResult
        Page<Clazz> p = (Page<Clazz>) clazzList;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @LogOperation
    @Override
    public void deleteById(Integer id) {
        // 1. 根据班级 ID 去学生表查询，该班级下有没有学生
        Integer count = studentMapper.countByClazzId(id);

        // 2. 如果有学生 (数量大于 0)，直接抛出业务异常，中断后续代码执行
        if (count != null && count > 0) {
            throw new BusinessException("对不起, 该班级下有学生, 不能直接删除");
        }
        clazzMapper.deleteById(id);
    }


    @LogOperation
    @Override
    public void save(Clazz clazz) {
        clazz.setCreateTime(LocalDateTime.now());
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.save(clazz);
    }
    @LogOperation
    @Override
    public Clazz getClazzById(Integer id) {
        return clazzMapper.getClazzById(id);
    }

    @Override
    public void updateById(Clazz clazz) {
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.updateById(clazz);
    }

    @Override
    public List<Clazz> getClazzs() {
        return clazzMapper.getClazzs();
    }
}
