package com.xhz.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xhz.mapper.EmpExprMapper;
import com.xhz.mapper.EmpMapper;
import com.xhz.mapper.SysRoleMapper;
import com.xhz.mapper.SysRolePermissionMapper;
import com.xhz.pojo.*;
import com.xhz.pojo.param.EmpAddParam;
import com.xhz.pojo.param.EmpExprParam;
import com.xhz.service.EmpLogService;
import com.xhz.service.EmpService;
import com.xhz.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 员工管理 — Service 层
 * <p>
 * 职责：核心业务逻辑、实体组装、Mapper 操作、事务管理。
 * 返回纯数据（实体/影响行数），绝不返回大模型自然语言战报。
 */
@Slf4j
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private EmpExprMapper empExprMapper;

    @Autowired
    private EmpLogService empLogService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    // ==================== Web 表单方法（保持原样） ====================

    @Cacheable(value = "empPage", key = "#empQueryParam")
    public PageResult page(EmpQueryParam empQueryParam) {
        PageHelper.startPage(empQueryParam.getPage(), empQueryParam.getPageSize());
        List<Emp> empList = empMapper.list(empQueryParam);
        Page<Emp> p = (Page<Emp>) empList;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @CacheEvict(value = {"emplist", "empPage"}, allEntries = true)
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Emp emp) {
        try {
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.insert(emp);

            Integer empId = emp.getId();
            List<EmpExpr> exprList = emp.getExprList();
            if (!CollectionUtils.isEmpty(exprList)) {
                exprList.forEach(empExpr -> empExpr.setEmpId(empId));
                empExprMapper.insertBatch(exprList);
            }
        } finally {
            EmpLog empLog = new EmpLog(null, LocalDateTime.now(), emp.toString());
            empLogService.insertLog(empLog);
        }
    }

    @Override
    public Emp getInfo(Integer id) {
        return empMapper.getById(id);
    }

    @CacheEvict(value = {"emplist", "empPage"}, allEntries = true)
    @Transactional
    @Override
    public void update(Emp emp) {
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.updateById(emp);

        empExprMapper.deleteByEmpIds(Arrays.asList(emp.getId()));

        Integer empId = emp.getId();
        List<EmpExpr> exprList = emp.getExprList();
        if (!CollectionUtils.isEmpty(exprList)) {
            exprList.forEach(empExpr -> empExpr.setEmpId(empId));
            empExprMapper.insertBatch(exprList);
        }
    }

    @Cacheable(value = "emplist", key = "'list'")
    @Override
    public List<Emp> getEmps() {
        return empMapper.getEmps();
    }

    @Override
    public LoginInfo login(Emp emp) {
        Emp empLogin = empMapper.getUsernameAndPassword(emp);
        if (empLogin != null) {
            // 1. deptId → 角色映射（学工部=1、教研部=2 → ADMIN）
            String roleCode = mapDeptToRole(empLogin.getDeptId());
            List<String> permissions = Collections.emptyList();

            // 2. 查 RBAC 表加载权限（表不存在时降级为空权限，不阻塞登录）
            try {
                SysRole role = sysRoleMapper.findByRoleCode(roleCode);
                if (role != null) {
                    permissions = sysRolePermissionMapper.findPermCodesByRoleId(role.getId());
                }
            } catch (Exception e) {
                log.warn("加载角色权限失败（可能 RBAC 表未创建），降级为空权限：{}", e.getMessage());
            }

            // 3. 将角色+权限打入 JWT
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", empLogin.getId());
            dataMap.put("username", empLogin.getUsername());
            dataMap.put("role", roleCode);
            dataMap.put("permissions", permissions);
            String jwt = JwtUtils.generateJwt(dataMap);

            log.info("登录成功：userId={}, username={}, deptId={}, role={}, permissions={}",
                    empLogin.getId(), empLogin.getUsername(), empLogin.getDeptId(), roleCode, permissions);

            return new LoginInfo(empLogin.getId(), empLogin.getUsername(), empLogin.getName(), jwt);
        }
        return null;
    }

    /**
     * 根据 emp.deptId 映射角色标识。
     * <pre>
     *   1（学工部）、2（教研部）→ ADMIN（查询+写）
     *   其他                     → USER（仅部门/班级查询）
     * </pre>
     */
    private String mapDeptToRole(Integer deptId) {
        if (deptId == null) return "USER";
        return switch (deptId) {
            case 1, 2 -> "ADMIN";
            default   -> "USER";
        };
    }

    // ==================== AI Tool Calling 专用方法 ====================

    /**
     * AI 工具调用：新员工入职登记
     * <p>
     * 完整链路：EmpAddParam 校验 → Emp 实体组装 → EmpExpr 集合组装 → 事务写入
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Emp addEmp(EmpAddParam param) {
        // 1. 参数校验 — 不通过直接抛异常，由 Tool 层 catch 转友好提示
        if (param == null || param.username() == null || param.name() == null || param.phone() == null) {
            throw new RuntimeException("未收到有效的员工核心信息，请提供用户名、姓名和手机号。");
        }

        // 2. EmpAddParam → Emp 实体组装
        Emp emp = assembleEmp(param);

        // 3. 保存主表基本信息（MyBatis useGeneratedKeys 自动回填 emp.id）
        empMapper.insert(emp);
        Integer empId = emp.getId();

        // 4. 组装并保存子表工作经历
        int exprCount = 0;
        if (param.exprList() != null && !param.exprList().isEmpty()) {
            List<EmpExpr> exprEntityList = assembleExprList(param.exprList(), empId);
            exprCount = empExprMapper.insertBatch(exprEntityList);
        }

        // 5. 将 exprList 回填到 emp 中，方便 Tool 层取用
        //    注：此处不完整回填，Tool 层通过 exprCount 参数获取数目
        log.info("AI 员工保存成功，员工ID：{}，姓名：{}，工作经历条数：{}", empId, emp.getName(), exprCount);

        return emp;
    }


    /**
     * 核心业务：批量删除员工及关联工作经历
     * （供 Controller 和 AI Tool 层复用）
     */
    @CacheEvict(value = {"emplist", "empPage"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteEmpByIds(List<Integer> ids) {
        // 1. 参数校验
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("未提供有效的员工ID列表。"); // 抛出标准参数异常
        }

        // 2. 先删子表（工作经历）- 顺序绝不能错
        empExprMapper.deleteByEmpIds(ids);

        // 3. 再删主表（员工基本信息）
        int empCount = empMapper.deleteByIds(ids);

        // 4. 记录核心业务日志
        log.info("业务操作：批量删除员工成功，请求删除 {} 个，实际删除 {} 个", ids.size(), empCount);

        // 5. 返回实际删除的主表数量
        return empCount;
    }

    // ==================== Assembler（实体组装器 — 从 Tool 层迁移至此） ====================

    /**
     * EmpAddParam → Emp 实体
     */
    private Emp assembleEmp(EmpAddParam param) {
        Emp emp = new Emp();
        emp.setUsername(param.username());
        emp.setName(param.name());
        emp.setGender(param.gender());
        emp.setPhone(param.phone());
        emp.setJob(param.job());
        emp.setSalary(param.salary());
        emp.setImage(param.image());

        // 系统默认值
        emp.setPassword("123456");
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());

        // 安全转换大模型生成的 String 日期
        if (param.entryDate() != null && !param.entryDate().isBlank()) {
            try {
                emp.setEntryDate(LocalDate.parse(param.entryDate()));
            } catch (Exception e) {
                log.warn("AI 生成的入职日期格式异常: {}", param.entryDate());
            }
        }
        return emp;
    }

    /**
     * List<EmpExprParam> → List<EmpExpr> 实体集合
     */
    private List<EmpExpr> assembleExprList(List<EmpExprParam> requestList, Integer empId) {
        List<EmpExpr> exprList = new ArrayList<>();
        for (EmpExprParam req : requestList) {
            EmpExpr expr = new EmpExpr();
            expr.setEmpId(empId);
            expr.setCompany(req.company());
            expr.setJob(req.job());

            try {
                if (req.begin() != null && !req.begin().isBlank()) {
                    expr.setBegin(LocalDate.parse(req.begin()));
                }
                if (req.end() != null && !req.end().isBlank()) {
                    expr.setEnd(LocalDate.parse(req.end()));
                }
            } catch (Exception e) {
                log.warn("AI 生成的工作经历日期格式异常, begin:{}, end:{}", req.begin(), req.end());
            }
            exprList.add(expr);
        }
        return exprList;
    }
}
