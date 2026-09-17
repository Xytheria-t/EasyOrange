package com.cartethyia.easyorange.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 通用分页请求参数
 * <p>
 * 三条构造路径都必须拿到合法分页，缺一不可：
 * <ul>
 *   <li>字段初始值 — no-args 构造（query-param 绑定在**一个参数都没传**时不会调用任何 setter，
 *       子类字段会保持构造器设的值，这是最容易漏的一条）</li>
 *   <li>setter — Jackson 反序列化 / Spring 数据绑定（显式传 null 或非法值）</li>
 *   <li>{@code @Builder.Default} — Builder 路径（不设值时取默认值）</li>
 * </ul>
 * 只做 setter 规整会留下 no-args 路径的 null，下游一拆箱就是 500。
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

    @Builder.Default
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @Builder.Default
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 " + MAX_PAGE_SIZE)
    private Integer pageSize = DEFAULT_PAGE_SIZE;

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
