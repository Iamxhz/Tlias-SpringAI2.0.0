package com.xhz.pojo;

import com.xhz.pojo.param.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Emp implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(groups = ValidationGroups.Update.class, message = "员工 ID 不能为空")
    private Integer id; //ID,主键

    @NotBlank(groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class}, message = "用户名不能为空")
    private String username; //用户名

    private String password; //密码

    @NotBlank(groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class}, message = "姓名不能为空")
    private String name; //姓名

    private Integer gender; //性别, 1:男, 2:女

    @Pattern(regexp = "^1[3-9]\\d{9}$",
             groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class},
             message = "手机号格式错误")
    private String phone; //手机号

    private Integer job; //职位, 1:班主任,2:讲师,3:学工主管,4:教研主管,5:咨询师
    private Integer salary; //薪资
    private String image; //头像
    private LocalDate entryDate; //入职日期
    private Integer deptId; //关联的部门ID
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间

    //封装部门名称数
    private String deptName; //部门名称

    //封装员工工作经历信息
    private List<EmpExpr> exprList;

}
