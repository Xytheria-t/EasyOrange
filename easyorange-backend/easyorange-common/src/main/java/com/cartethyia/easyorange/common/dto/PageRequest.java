package com.cartethyia.easyorange.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 通用分页请求参数
 * <p>
 * 规范化（normalization）在 setter 和全参构造器中自动完成，
 * Jackson 反序列化路径（no-args + setters）和子类 {@code super()} 路径均无需手动 normalized()。
 * </p>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
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
     * 全参构造器，自动规整 pageNum/pageSize（通过 setter 确保合法值）。
     * 替代 Lombok {@code @AllArgsConstructor}，供子类 {@code super()} 调用。
     */
    public PageRequest(Integer pageNum, Integer pageSize, String sortField, String sortDirection) {
        setPageNum(pageNum);
        setPageSize(pageSize);
        this.sortField = sortField;
        this.sortDirection = sortDirection;
    }

    // ——— Setter 级自动规整（Jackson 反序列化路径） ———

    public void setPageNum(Integer pageNum) {
        this.pageNum = (pageNum == null || pageNum < DEFAULT_PAGE_NUM) ? DEFAULT_PAGE_NUM : pageNum;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
