package com.xhz.exception;

/**
 * 自定义业务异常
 */
public class BusinessException extends RuntimeException {

    // 构造方法，接收我们需要提示给前端的错误信息
    public BusinessException(String message) {
        super(message);
    }
}