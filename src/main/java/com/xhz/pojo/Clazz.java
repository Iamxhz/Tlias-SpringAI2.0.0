package com.xhz.pojo;

import com.xhz.pojo.param.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clazz {
    @NotNull(groups = ValidationGroups.Update.class, message = "班级 ID 不能为空")
    private Integer id; //ID

    @NotBlank(groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class}, message = "班级名称不能为空")
    private String name; //班级名称

    private String room; //班级教室
    private LocalDate beginDate; //开课时间
    private LocalDate endDate; //结课时间
    private Integer masterId; //班主任
    private Integer subject; //学科
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间

    private String masterName; //班主任姓名
    private String status; //班级状态 - 未开班 , 在读 , 已结课
}
