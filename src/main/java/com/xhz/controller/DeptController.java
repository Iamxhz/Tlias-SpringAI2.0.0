package com.xhz.controller;

import com.xhz.pojo.Dept;
import com.xhz.pojo.Result;
import com.xhz.pojo.param.ValidationGroups;
import com.xhz.service.DeptService;
import com.xhz.anno.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * 部门管理控制器
 */
@Slf4j
@RequestMapping("/depts")
@RestController
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 查询部门列表
     */
    @RequirePermission("dept:query")
    @GetMapping
    public Result<List<Dept>> list(){
        log.info("查询部门列表");
        List<Dept> deptList = deptService.findAll();
        return Result.success(deptList);
    }

    /**
     * 根据id删除部门 - delete http://localhost:8080/depts?id=1
     */
    @RequirePermission("dept:delete")
    @DeleteMapping
    public Result<Void> delete(Integer id){
        log.info("根据id删除部门, id: {}" , id);
        deptService.deleteById(id);
        return Result.success();
    }

    /**
     * 新增部门 - POST http://localhost:8080/depts   请求参数：{"name":"研发部"}
     */
    @RequirePermission("dept:save")
    @PostMapping
    public Result<Void> save(@Validated(ValidationGroups.Insert.class) @RequestBody Dept dept){
        log.info("新增部门, dept: {}" , dept);
        deptService.save(dept);
        return Result.success();
    }

    /**
     * 根据ID查询 - GET http://localhost:8080/depts/1
     */
    @RequirePermission("dept:query")
    @GetMapping("/{id}")
    public Result<Dept> getById(@PathVariable Integer id){
        log.info("根据ID查询, id: {}" , id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 修改部门 - PUT http://localhost:8080/depts  请求参数：{"id":1,"name":"研发部"}
     */
    @RequirePermission("dept:update")
    @PutMapping
    public Result<Void> update(@Validated(ValidationGroups.Update.class) @RequestBody Dept dept){
        log.info("修改部门, dept: {}" , dept);
        deptService.update(dept);
        return Result.success();
    }
}
