package com.xhz.controller;


import com.xhz.pojo.PageResult;
import com.xhz.pojo.Result;
import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;
import com.xhz.pojo.param.ValidationGroups;
import com.xhz.service.StudentService;
import com.xhz.anno.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @RequirePermission("student:query")
    @GetMapping
    public Result<PageResult<Student>> getStuPage(StudentQueryParam queryParam){
        PageResult<Student> page = studentService.getStuPage(queryParam);
        return Result.success(page);
    }
    @RequirePermission("student:query")
    @GetMapping("/{id}")
    public Result<Student> getStudentById(@PathVariable Integer id){
        Student student = studentService.getStuById(id);
        return Result.success(student);
    }
    @RequirePermission("student:delete")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable List<Integer> ids){
        studentService.deleteById(ids);
        return Result.success();
    }
    @RequirePermission("student:save")
    @PostMapping
    public Result<Void> save(@Validated(ValidationGroups.Insert.class) @RequestBody Student student){
        studentService.save(student);
        return Result.success();
    }
    @RequirePermission("student:update")
    @PutMapping
    public Result<Void> update(@Validated(ValidationGroups.Update.class) @RequestBody Student student){
        studentService.updateById(student);
        return Result.success();
    }
    @RequirePermission("student:violation")
    @PutMapping("/violation/{id}/{score}")
    public Result<Void> updateViolation(@PathVariable Integer id,@PathVariable Short score){

        studentService.updateViolation(id,score);
        return Result.success();
    }

}
