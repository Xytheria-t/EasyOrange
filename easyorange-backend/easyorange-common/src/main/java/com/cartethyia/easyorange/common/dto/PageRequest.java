package com.cartethyia.easyorange.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * 通用分页请求参数
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 当前页码，从 1 开始
     */
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum;

    /**
     * 每页条数，最大 100 条
     */
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 " + MAX_PAGE_SIZE)
    private Integer pageSize;

    /**
     * 排序字段（白名单校验，防止 SQL 注入）
     */
    private String sortField;

    /**
     * 排序方向：asc / desc
     */
    @Pattern(regexp = "^(asc|desc|ASC|DESC)?$", message = "排序方向必须为 asc 或 desc")
    private String sortDirection;

    /**
     * 规范化分页参数，将 null 或非法值替换为默认值
     * <p>
     * 注意：此方法修改对象状态（mutable）。
     * </p>
     */
    public void normalize() {
        this.pageNum = (pageNum == null || pageNum < DEFAULT_PAGE_NUM) ? DEFAULT_PAGE_NUM : pageNum;
        this.pageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 校验排序字段是否在白名单内（调用方应传入允许的字段列表）
     *
     * @param allowedFields 允许的排序字段白名单
     * @return 合法的排序字段名，若不合法则返回 null
     */
    public String validateSortField(Set<String> allowedFields) {
        if (sortField == null || sortField.isBlank()) {
            return null;
        }
        return allowedFields.contains(sortField) ? sortField : null;
    }

    /**
     * 是否为升序
     */
    public boolean isAsc() {
        return sortDirection == null || "asc".equalsIgnoreCase(sortDirection);
    }

    /**
     * 计算 SQL OFFSET 值
     *
     * @return (pageNum - 1) * pageSize
     */
    public int getOffset() {
        int pn = (pageNum != null) ? pageNum : DEFAULT_PAGE_NUM;
        int ps = (pageSize != null) ? pageSize : DEFAULT_PAGE_SIZE;
        return (pn - 1) * ps;
    }
}
