package com.xhz.pojo.param;

/**
 * 参数校验分组接口 — 区分新增与修改场景的差异化校验规则。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 实体类中：
 * @NotNull(groups = Update.class, message = "ID 不能为空")
 * private Integer id;
 *
 * @NotBlank(groups = {Insert.class, Update.class}, message = "用户名不能为空")
 * private String username;
 *
 * // Controller 中：
 * @PostMapping
 * public Result save(@Validated(Insert.class) @RequestBody Emp emp) { ... }
 *
 * @PutMapping
 * public Result update(@Validated(Update.class) @RequestBody Emp emp) { ... }
 * }</pre>
 *
 * @see org.springframework.validation.annotation.Validated
 * @see jakarta.validation.Valid
 */
public final class ValidationGroups {

    private ValidationGroups() {
        // 工具类，禁止实例化
    }

    /** 新增场景：主键 ID 应为空，必填字段严格校验 */
    public interface Insert {}

    /** 修改场景：主键 ID 必填，允许部分字段为空（仅更新变更字段） */
    public interface Update {}
}
