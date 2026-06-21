package com.xhz.service;

import com.xhz.pojo.JobOption;
import com.xhz.pojo.StuOption;

import java.util.List;
import java.util.Map;

public interface ReportService {
    /**
     * 统计各个职位的员工人数
     * @return
     */
    JobOption getEmpJobData();

    List<Map> getEmpGenderData();

    List<Map> getStudentDegreeData();

    StuOption getStudentCountData();
}