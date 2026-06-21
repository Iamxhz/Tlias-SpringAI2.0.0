package com.xhz.pojo.param;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record EmpExprParam(
        @JsonPropertyDescription("曾就职的公司名称，若无则留空。")
        String company,

        @JsonPropertyDescription("担任的职位，若无则留空。")
        String job,

        @JsonPropertyDescription("开始日期，格式 yyyy-MM-dd，如只提供年份默认取当年1月1日。若无则留空。")
        String begin,

        @JsonPropertyDescription("结束日期，格式 yyyy-MM-dd，若说「至今」请输出调用当天的日期。若无则留空。")
        String end
) {}