package com.xhz.service.impl;

import com.xhz.anno.LogOperation;
import com.xhz.exception.BusinessException;
import com.xhz.mapper.DeptMapper;
import com.xhz.mapper.EmpMapper;
import com.xhz.pojo.Dept;
import com.xhz.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private EmpMapper empMapper;

    /**
     * 查询全部部门 — 缓存 30 分钟（部门变动极少）
     */
    @Cacheable(value = "dept", key = "'list'")
    @Override
    public List<Dept> findAll() {
        return deptMapper.findAll();
    }

    /**
     * 根据id删除部门 — 清部门缓存
     */
    @CacheEvict(value = "dept", allEntries = true)
    @Override
    @LogOperation
    public void deleteById(Integer id) {
        Integer count =empMapper.countDeptByID(id);
        if(count != null && count > 0){
            throw new BusinessException("部门下有员工，不能删除");
        }
        deptMapper.deleteById(id);
    }

    @CacheEvict(value = "dept", allEntries = true)
    @Override
    public void save(Dept dept) {
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
    }

    @Override
    public Dept getById(Integer id) {
        return deptMapper.getById(id);
    }

    @CacheEvict(value = "dept", allEntries = true)
    @Override
    public void update(Dept dept) {
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.update(dept);
    }
}
