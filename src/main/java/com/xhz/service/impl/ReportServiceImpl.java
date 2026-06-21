package com.xhz.service.impl;

import com.xhz.mapper.EmpMapper;
import com.xhz.pojo.JobOption;
import com.xhz.pojo.StuOption;
import com.xhz.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private EmpMapper empMapper;

    @Override
    public JobOption getEmpJobData() {
        List<Map<String,Object>> list = empMapper.countEmpJobData();
        List<Object> jobList = list.stream().map(dataMap -> dataMap.get("pos")).toList();
        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("total")).toList();
        return new JobOption(jobList, dataList);
    }

    @Override
    public List<Map> getEmpGenderData() {
        return empMapper.countEmpGenderData();
    }

    @Override
    public List<Map> getStudentDegreeData() {
        return empMapper.countStudentDegreeData();
    }

    @Override
    public StuOption getStudentCountData() {
        List<Map<String,Object>> list = empMapper.getStudentCountData();
        List<Object> jobList = list.stream().map(dataMap -> dataMap.get("pos")).toList();
        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("total")).toList();
        return new StuOption(jobList, dataList);
    }
}