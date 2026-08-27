package com.xhz.controller;

import com.xhz.pojo.Emp;
import com.xhz.pojo.EmpQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.Result;
import com.xhz.pojo.param.ValidationGroups;
import com.xhz.service.EmpService;
import com.xhz.anno.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/emps")
public class EmpController {
    @Autowired
    private EmpService empService;

    @RequirePermission("emp:query")
    @GetMapping("/list")
    public Result<List<Emp>> getEmps(){
        log.info("查询员工列表");
        List<Emp> list = empService.getEmps();
        return Result.success(list);
    }


    @RequirePermission("emp:query")
    @GetMapping
    public Result<PageResult> page(EmpQueryParam empQueryParam) {
        log.info("查询请求参数： {}", empQueryParam);
        PageResult pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }
    /**
     * 添加员工
     */
    @RequirePermission("emp:save")
    @PostMapping
    public Result<Void> save(@Validated(ValidationGroups.Insert.class) @RequestBody Emp emp){
        log.info("请求参数emp: {}", emp);
        empService.save(emp);
        return Result.success();
    }
    /**
     * 批量删除员工
     */
    @RequirePermission("emp:delete")
    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Integer> ids){
        log.info("批量删除部门: ids={} ", ids);
        empService.deleteEmpByIds(ids);
        return Result.success();
    }
    /**
     * 查询回显
     */
    @RequirePermission("emp:query")
    @GetMapping("/{id}")
    public Result<Emp> getInfo(@PathVariable Integer id){
        log.info("根据id查询员工的详细信息");
        Emp emp  = empService.getInfo(id);
        return Result.success(emp);
    }
    /**
     * 更新员工信息
     */
    @RequirePermission("emp:update")
    @PutMapping
    public Result<Void> update(@Validated(ValidationGroups.Update.class) @RequestBody Emp emp){
        log.info("修改员工信息, {}", emp);
        empService.update(emp);
        return Result.success();
    }
}
