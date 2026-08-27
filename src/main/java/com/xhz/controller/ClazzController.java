package com.xhz.controller;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.Result;
import com.xhz.pojo.param.ValidationGroups;
import com.xhz.service.ClazzService;
import com.xhz.anno.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clazzs")
public class ClazzController {
    @Autowired
    private ClazzService clazzService;
    @RequirePermission("clazz:query")
    @GetMapping
    public Result<PageResult> getClazzsPage(ClazzQueryParam clazzQueryParam) {
        PageResult clazzs =clazzService.getClazzsPage(clazzQueryParam);
        return Result.success(clazzs);
    }
    @RequirePermission("clazz:query")
    @GetMapping("/{id}")
    public Result<Clazz> getClazzById(@PathVariable Integer id) {
        Clazz clazz = clazzService.getClazzById(id);
        return Result.success(clazz);
    }
    @RequirePermission("clazz:query")
    @GetMapping("/list")
    public Result<List<Clazz>> getClazzs() {
        List<Clazz> clazzs = clazzService.getClazzs();
        return Result.success(clazzs);
    }
    @RequirePermission("clazz:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        clazzService.deleteById(id);
        return Result.success();
    }
    @RequirePermission("clazz:save")
    @PostMapping
    public Result<Void> save(@Validated(ValidationGroups.Insert.class) @RequestBody Clazz clazz) {
        clazzService.save(clazz);
        return Result.success();
    }
    @RequirePermission("clazz:update")
    @PutMapping
    public Result<Void> update(@Validated(ValidationGroups.Update.class) @RequestBody Clazz clazz) {
        clazzService.updateById(clazz);
        return Result.success();
    }
}
