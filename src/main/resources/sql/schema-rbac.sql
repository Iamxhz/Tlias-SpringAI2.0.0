-- ================================================================
-- Phase 2 RBAC 建表 DDL（方案 B：2 表轻量 RBAC）
-- 角色表 sys_role + 角色-权限关联表 sys_role_permission
-- 用户基础复用 emp 表，通过 emp.job 映射角色
-- ================================================================

CREATE TABLE IF NOT EXISTS sys_role (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(30)  NOT NULL UNIQUE COMMENT '角色标识，如 ADMIN、USER',
    role_name   VARCHAR(30)  NOT NULL COMMENT '角色名称',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统角色表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id    INT          NOT NULL COMMENT '角色 ID',
    perm_code  VARCHAR(100) NOT NULL COMMENT '权限码，如 emp:delete',
    PRIMARY KEY (role_id, perm_code)
) COMMENT '角色-权限关联表';
