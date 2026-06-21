package com.xhz.pojo.param;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record EmpAddParam(
        @JsonPropertyDescription("登录用户名，2-20个字。必须填写。")
        String username,

        @JsonPropertyDescription("员工姓名，2-10个字。必须填写。")
        String name,

        @JsonPropertyDescription("性别。男传 1，女传 2。必须填写。")
        Integer gender,

        @JsonPropertyDescription("11位手机号。必须填写。")
        String phone,

        @JsonPropertyDescription("职位。班主任传1, 讲师传2, 学工主管传3, 教研主管传4, 咨询师传5。若无则留空。")
        Integer job,

        @JsonPropertyDescription("薪资，单位元（整数）。若无则留空。")
        Integer salary,

        @JsonPropertyDescription("所属部门名称，例如：教研部、学工部。若无则留空。")
        String deptName,

        @JsonPropertyDescription("入职日期，格式 yyyy-MM-dd。若用户说「明天」需计算具体日期。若无则留空。")
        String entryDate,

        @JsonPropertyDescription("头像图片的URL，若无则留空。")
        String image,

        @JsonPropertyDescription("历史工作经历列表，无则留空。")
        List<EmpExprParam> exprList // 直接引用外部独立的 Record
) {
}