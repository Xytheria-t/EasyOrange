package com.cartethyia.easyorange.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页数据记录列表
     */
    private List<T> records;

    /**
     * 符合条件的总记录数
     */
    private Long total;

    /**
     * 当前页码（从 1 开始）
     */
    private Integer current;

    /**
     * 每页显示条数
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer pages;

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        int pages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        return new PageResult<>(
                records != null ? records : Collections.emptyList(),
                total,
                pageNum,
                pageSize,
                pages
        );
    }

    /**
     * 从 MyBatis-Plus IPage 对象转换为 PageResult
     * <p>
     * 自动提取 records、total、current、size、pages 字段，
     * 避免手动传参导致的错误。
     * </p>
     *
     * <pre>{@code
     * // 用法示例
     * IPage<User> page = userMapper.selectPage(new Page<>(1, 10), wrapper);
     * PageResult<UserVO> result = PageResult.fromIPage(page, voList);
     * }</pre>
     *
     * @param page  MyBatis-Plus IPage 对象
     * @param data  转换后的数据列表（通常经过 VO 转换）
     * @param <T>   数据类型
     * @return PageResult 对象
     */
    public static <T> PageResult<T> fromIPage(com.baomidou.mybatisplus.core.metadata.IPage<?> page, List<T> data) {
        if (page == null) {
            return empty(1, 10);
        }
        return new PageResult<>(
                data != null ? data : Collections.emptyList(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                (int) page.getPages()
        );
    }

    /**
     * 从 MyBatis-Plus IPage 对象直接转换（数据不转换）
     * <p>
     * 适用于 DO 直接返回的场景，如果需要进行 VO 转换请使用 {@link #fromIPage(com.baomidou.mybatisplus.core.metadata.IPage, List)}。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> fromIPage(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        if (page == null) {
            return empty(1, 10);
        }
        return new PageResult<>(
                page.getRecords() != null ? page.getRecords() : Collections.emptyList(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                (int) page.getPages()
        );
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize, 0);
    }

    public boolean hasData() {
        return records != null && !records.isEmpty();
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return current != null && pages != null && current < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return current != null && current > 1;
    }
}
