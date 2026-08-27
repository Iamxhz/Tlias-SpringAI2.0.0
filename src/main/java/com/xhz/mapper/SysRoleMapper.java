package com.xhz.mapper;

import com.xhz.pojo.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 角色表 Mapper — sys_role
 */
@Mapper
public interface SysRoleMapper {

    @Select("SELECT id, role_code, role_name FROM sys_role WHERE role_code = #{roleCode}")
    SysRole findByRoleCode(String roleCode);
}
