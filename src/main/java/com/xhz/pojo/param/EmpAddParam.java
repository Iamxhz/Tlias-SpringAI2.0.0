package com.xhz.pojo.param;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 员工新增入参 — 同时服务于 REST Controller（@Valid 校验）和 AI @Tool Function Calling（JSON Schema 生成）。
 *
 * <p>{@link JsonPropertyDescription} 为 LLM 提供参数语义描述；
 * {@link NotBlank} / {@link Pattern} 等为 REST API 提供入参校验。
 */
public record EmpAddParam(
        @JsonPropertyDescription("登录用户名，2-20个字。必须填写。")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 20, message = "用户名长度需在 2-20 之间")
        String username,

        @JsonPropertyDescription("员工姓名，2-10个字。必须填写。")
        @NotBlank(message = "姓名不能为空")
        @Size(min = 2, max = 10, message = "姓名长度需在 2-10 之间")
        String name,

        @JsonPropertyDescription("性别。男传 1，女传 2。必须填写。")
        Integer gender,

        @JsonPropertyDescription("11位手机号。必须填写。")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误，需为 11 位有效号码")
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
        List<EmpExprParam> exprList
) {
}