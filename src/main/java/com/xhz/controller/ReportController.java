package com.xhz.controller;

import com.xhz.pojo.JobOption;
import com.xhz.pojo.Result;
import com.xhz.pojo.StuOption;
import com.xhz.service.ReportService;
import com.xhz.anno.RequirePermission;
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
    @RequirePermission("report:query")
    @GetMapping("/empJobData")
    public Result<JobOption> getEmpJobData(){
        log.info("统计各个职位的员工人数");
        JobOption jobOption = reportService.getEmpJobData();
        return Result.success(jobOption);
    }
    /**
     * 统计员工性别信息
     */
    @RequirePermission("report:query")
    @GetMapping("/empGenderData")
    public Result<List<Map>> getEmpGenderData(){
        log.info("统计员工性别信息");
        List<Map> genderList = reportService.getEmpGenderData();
        return Result.success(genderList);
    }
    @RequirePermission("report:query")
    @GetMapping("/studentDegreeData")
    public Result<List<Map>> getStudentDegreeData(){
        log.info("统计学员性别信息");
        List<Map> degreeList = reportService.getStudentDegreeData();
        return Result.success(degreeList);
    }
    @RequirePermission("report:query")
    @GetMapping("/studentCountData")
    public Result<StuOption> getStudentCountData(){
        log.info("统计学员性别信息");
        StuOption stuOption = reportService.getStudentCountData();
        return Result.success(stuOption);
    }

}