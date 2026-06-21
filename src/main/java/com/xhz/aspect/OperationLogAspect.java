package com.xhz.aspect;

import com.xhz.anno.LogOperation;
import com.xhz.mapper.OperateLogMapper;
import com.xhz.pojo.OperateLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    // 环绕通知
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation log) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 当前时间
        long endTime = System.currentTimeMillis();
        // 耗时
        long costTime = endTime - startTime;

        // 构建日志对象
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(getCurrentUserId()); // 需要实现 getCurrentUserId 方法
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));

        // 🌟 核心修复点：防范 void 方法返回 null 导致的空指针异常
        // 如果 result 不为空，就转成字符串；如果为空（说明是 void 方法），就存入 "无返回值" 或 "void"
        //operateLog.setReturnValue(result != null ? result.toString() : "无返回值");
        operateLog.setReturnValue(String.valueOf(result));

        operateLog.setCostTime(costTime);

        // 插入日志
        operateLogMapper.insert(operateLog);

        return result;
    }

    // 示例方法，获取当前用户ID
    private int getCurrentUserId() {
        // 这里应该根据实际情况从认证信息中获取当前登录用户的ID
        return 1; // 示例返回值
    }
}