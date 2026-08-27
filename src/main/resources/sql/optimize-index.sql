-- ================================================================
-- Phase 4 索引优化 — emp 表慢 SQL 分析
-- ================================================================

-- emp 表当前索引
-- PRIMARY KEY (id)
-- UNIQUE KEY  username (username)
-- UNIQUE KEY  phone (phone)
-- KEY          fk_dept_id (dept_id)

-- ================================================================
-- 1. 入职日期索引 — 分页查询 WHERE e.entry_date BETWEEN ? AND ?
--    如果员工表数据量大（> 10w），这个索引可以显著加速日期范围筛选
-- ================================================================
-- ALTER TABLE emp ADD INDEX idx_entry_date (entry_date);

-- 验证：
-- EXPLAIN SELECT e.*, d.name deptName FROM emp e LEFT JOIN dept d ON e.dept_id = d.id
-- WHERE e.entry_date BETWEEN '2024-01-01' AND '2024-12-31';

-- ================================================================
-- 2. 部门+入职日期复合索引 — 当部门筛选 + 日期范围同时存在时生效
--    先按 dept_id 过滤再按 entry_date 范围扫描
-- ================================================================
-- ALTER TABLE emp ADD INDEX idx_dept_entry (dept_id, entry_date);

-- 验证：
-- EXPLAIN SELECT e.*, d.name deptName FROM emp e LEFT JOIN dept d ON e.dept_id = d.id
-- WHERE e.dept_id = 1 AND e.entry_date BETWEEN '2024-01-01' AND '2024-12-31';
