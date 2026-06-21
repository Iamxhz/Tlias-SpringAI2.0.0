package com.xhz.pojo;

import lombok.Data;

@Data
public class StudentQueryParam {

    private Integer page = 1; //页码
    private Integer pageSize = 10; //每页展示记录数

    private String name; //姓名
    private Integer degree; //最高学历
    private Integer clazzId; //所属班级

}
