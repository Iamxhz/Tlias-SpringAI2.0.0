package com.xhz.pojo;

import com.xhz.pojo.param.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dept implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(groups = ValidationGroups.Update.class, message = "部门 ID 不能为空")
    private Integer id;

    @NotBlank(groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class}, message = "部门名称不能为空")
    private String name;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}