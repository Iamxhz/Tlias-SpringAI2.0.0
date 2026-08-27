package com.xhz.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联表 Mapper — sys_role_permission
 */
@Mapper
public interface SysRolePermissionMapper {

    /**
     * 根据角色 ID 查询该角色拥有的所有权限码
     */
    @Select("SELECT perm_code FROM sys_role_permission WHERE role_id = #{roleId}")
    List<String> findPermCodesByRoleId(Integer roleId);
}
