package com.xhz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统角色实体 — 对应 sys_role 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysRole {
    private Integer id;
    private String roleCode;   // 角色标识：ADMIN / USER
    private String roleName;   // 角色名称：管理员 / 普通用户
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
