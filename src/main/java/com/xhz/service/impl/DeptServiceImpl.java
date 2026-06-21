package com.xhz.service.impl;

import com.xhz.anno.LogOperation;
import com.xhz.exception.BusinessException;
import com.xhz.mapper.DeptMapper;
import com.xhz.mapper.EmpMapper;
import com.xhz.pojo.Dept;
import com.xhz.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private EmpMapper empMapper;
    @Override
    public List<Dept> findAll() {
        return deptMapper.findAll();
    }

    /**
     * 根据id删除部门
     */
    @Override
    @LogOperation //自定义注解（表示：当前方法属于目标方法）
    public void deleteById(Integer id) {
        Integer count =empMapper.countDeptByID(id);
        if(count != null && count > 0){
            throw new BusinessException("部门下有员工，不能删除");
        }
        deptMapper.deleteById(id);
    }

    @Override
    public void save(Dept dept) {
        //补全基础属性
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        //保存部门
        deptMapper.insert(dept);
    }

    @Override
    public Dept getById(Integer id) {
        return deptMapper.getById(id);
    }

    @Override
    public void update(Dept dept) {
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.update(dept);
    }
}
