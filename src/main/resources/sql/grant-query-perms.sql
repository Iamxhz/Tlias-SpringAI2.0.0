-- 已有库补查询权限（可重复执行）
-- ADMIN=1 可查员工/学员/班级/部门/报表；USER=2 仅部门、班级

INSERT IGNORE INTO sys_role_permission (role_id, perm_code) VALUES
(1, 'emp:query'),
(1, 'dept:query'),
(1, 'student:query'),
(1, 'clazz:query'),
(1, 'report:query'),
(2, 'dept:query'),
(2, 'clazz:query');
