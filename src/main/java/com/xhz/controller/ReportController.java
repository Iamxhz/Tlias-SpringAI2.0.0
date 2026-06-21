package com.xhz.controller;

import com.xhz.pojo.JobOption;
import com.xhz.pojo.Result;
import com.xhz.pojo.StuOption;
import com.xhz.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/report")
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 统计各个职位的员工人数
     */
    @GetMapping("/empJobData")
    public Result getEmpJobData(){
        log.info("统计各个职位的员工人数");
        JobOption jobOption = reportService.getEmpJobData();
        return Result.success(jobOption);
    }
    /**
     * 统计员工性别信息
     */
    @GetMapping("/empGenderData")
    public Result getEmpGenderData(){
        log.info("统计员工性别信息");
        List<Map> genderList = reportService.getEmpGenderData();
        return Result.success(genderList);
    }
    @GetMapping("/studentDegreeData")
    public Result getStudentDegreeData(){
        log.info("统计学员性别信息");
        List<Map> degreeList = reportService.getStudentDegreeData();
        return Result.success(degreeList);
    }
    @GetMapping("/studentCountData")
    public Result getStudentCountData(){
        log.info("统计学员性别信息");
        StuOption stuOption = reportService.getStudentCountData();
        return Result.success(stuOption);
    }

}