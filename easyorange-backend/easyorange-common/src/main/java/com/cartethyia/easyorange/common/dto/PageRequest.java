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

    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 " + MAX_PAGE_SIZE)
    private Integer pageSize;

    private String sortField;

    @Pattern(regexp = "^(asc|desc|ASC|DESC)?$", message = "排序方向必须为 asc 或 desc")
    private String sortDirection;

    /**
     * 返回规范化后的新实例（不可变模式）
     * <p>
     * 将 null 或非法值替换为默认值，返回新的 PageRequest 实例，不修改原对象。
     * </p>
     *
     * @return 规范化后的新 PageRequest 实例
     */
    public PageRequest normalized() {
        int normalizedPageNum = (pageNum == null || pageNum < DEFAULT_PAGE_NUM) ? DEFAULT_PAGE_NUM : pageNum;
        int normalizedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageRequest(normalizedPageNum, normalizedPageSize, this.sortField, this.sortDirection);
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

    public boolean isAsc() {
        return sortDirection == null || "asc".equalsIgnoreCase(sortDirection);
    }

    public int getOffset() {
        int pn = (pageNum != null) ? pageNum : DEFAULT_PAGE_NUM;
        int ps = (pageSize != null) ? pageSize : DEFAULT_PAGE_SIZE;
        return (pn - 1) * ps;
    }
}