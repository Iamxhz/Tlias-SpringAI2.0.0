package com.xhz.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 后端统一返回结果 — 泛型增强版
 *
 * <p>设计原则：
 * <ul>
 *   <li>{@code code = 1} 表示业务成功，{@code code = 0} 表示业务失败</li>
 *   <li>{@link #success()} / {@link #success(Object)} — 成功响应（无数据 / 带数据）</li>
 *   <li>{@link #fail(String)} — 业务失败（如校验不通过、权限不足）</li>
 *   <li>{@link #error(String)} — 系统级兜底错误（向后兼容旧代码）</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 编码：1 成功，0 失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    // ==================== 成功工厂方法 ====================

    /**
     * 成功响应（无数据体），如删除/更新操作。
     */
    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    /**
     * 成功响应（携带数据体），如查询操作。
     *
     * @param data 响应数据，可为 {@link PageResult}、实体对象或集合
     * @param <T>  数据类型
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.data = data;
        result.code = 1;
        result.msg = "success";
        return result;
    }

    // ==================== 失败工厂方法 ====================

    /**
     * 业务失败响应 — 用于校验失败、权限不足、业务规则拦截等可预期的失败场景。
     *
     * @param msg 面向用户的错误提示
     * @param <T> 数据类型（通常为 {@code Void}）
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

    /**
     * 系统级错误响应 — 向后兼容旧代码，语义与 {@link #fail(String)} 一致。
     *
     * @deprecated 新代码请使用 {@link #fail(String)}，语义更明确
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

}