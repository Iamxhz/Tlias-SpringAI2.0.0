package com.xhz.exception;

import com.xhz.pojo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 新增：专门捕获和处理业务异常 (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        // 这是我们预期内的业务拦截，通常不需要打印长串的报错堆栈
        // 直接取出我们在 Service 中传入的 message，返回给前端即可
        return Result.error(e.getMessage());
    }

    /**
     * 原有的：处理其他不可预知的系统级别的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result ex(Exception e){
        e.printStackTrace();//打印堆栈中的异常信息
        //捕获到异常之后，响应一个标准的Result
        return Result.error("对不起,操作失败,请联系管理员");
    }
}