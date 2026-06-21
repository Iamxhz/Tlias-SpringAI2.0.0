package com.xhz.controller;


import com.xhz.pojo.PageResult;
import com.xhz.pojo.Result;
import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;
import com.xhz.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @GetMapping
    public Result getStuPage(StudentQueryParam queryParam){
        PageResult<Student> page = studentService.getStuPage(queryParam);
        return Result.success(page);
    }
    @GetMapping("/{id}")
    public Result getStudentById(@PathVariable Integer id){
        Student student = studentService.getStuById(id);
        return Result.success(student);
    }
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable List<Integer> ids){
        studentService.deleteById(ids);
        return Result.success();
    }
    @PostMapping
    public Result save(@RequestBody Student student){
        studentService.save(student);
        return Result.success();
    }
    @PutMapping
    public Result update(@RequestBody Student student){
        studentService.updateById(student);
        return Result.success();
    }
    @PutMapping("/violation/{id}/{score}")
    public Result updateViolation(@PathVariable Integer id,@PathVariable Short score){

        studentService.updateViolation(id,score);
        return Result.success();
    }

}
