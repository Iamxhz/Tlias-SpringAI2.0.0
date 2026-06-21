package com.xhz.controller;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import com.xhz.pojo.PageResult;
import com.xhz.pojo.Result;
import com.xhz.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clazzs")
public class ClazzController {
    @Autowired
    private ClazzService clazzService;
    @GetMapping
    public Result getClazzsPage(ClazzQueryParam clazzQueryParam) {
        PageResult clazzs =clazzService.getClazzsPage(clazzQueryParam);
        return Result.success(clazzs);
    }
    @GetMapping("/{id}")
    public Result getClazzById(@PathVariable Integer id) {
        Clazz clazz = clazzService.getClazzById(id);
        return Result.success(clazz);
    }
    @GetMapping("/list")
    public Result getClazzs() {
        List<Clazz> clazzs = clazzService.getClazzs();
        return Result.success(clazzs);
    }
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        clazzService.deleteById(id);
        return Result.success();
    }
    @PostMapping
    public Result save(@RequestBody Clazz clazz) {
        clazzService.save(clazz);
        return Result.success();
    }
    @PutMapping
    public Result update(@RequestBody Clazz clazz) {
        clazzService.updateById(clazz);
        return Result.success();
    }
}
