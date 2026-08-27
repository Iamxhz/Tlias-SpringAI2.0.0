-- ================================================================
-- Phase 2 RBAC 种子数据
-- 角色：ADMIN（管理员）、USER（普通用户）
-- ADMIN 拥有查询 + 写权限；USER 仅部门/班级查询，不能看员工档案、学员名单和报表
-- ================================================================

INSERT INTO sys_role (id, role_code, role_name) VALUES
(1, 'ADMIN', '管理员'),
(2, 'USER', '普通用户');

-- 管理员：查询 + 写操作
INSERT INTO sys_role_permission (role_id, perm_code) VALUES
(1, 'emp:query'),
(1, 'emp:save'),
(1, 'emp:update'),
(1, 'emp:delete'),
(1, 'dept:query'),
(1, 'dept:save'),
(1, 'dept:update'),
(1, 'dept:delete'),
(1, 'student:query'),
(1, 'student:save'),
(1, 'student:update'),
(1, 'student:delete'),
(1, 'student:violation'),
(1, 'clazz:query'),
(1, 'clazz:save'),
(1, 'clazz:update'),
(1, 'clazz:delete'),
(1, 'report:query');

-- 普通用户：只能看部门/班级，不能查员工、学员、报表
INSERT INTO sys_role_permission (role_id, perm_code) VALUES
(2, 'dept:query'),
(2, 'clazz:query');
