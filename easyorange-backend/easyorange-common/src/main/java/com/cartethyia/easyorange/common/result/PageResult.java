package com.cartethyia.easyorange.common.result;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果封装
 */
public record PageResult<T>(
        List<T> records,
        Long total,
        Integer current,
        Integer size,
        Integer pages
) {

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

    public static <T> PageResult<T> fromIPage(com.baomidou.mybatisplus.core.metadata.IPage<?> page, List<T> data) {
        if (page == null) {
            return empty(1, 10);
        }
        return of(data, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    public static <T> PageResult<T> fromIPage(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        if (page == null) {
            return empty(1, 10);
        }
        return fromIPage(page, page.getRecords());
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(Collections.emptyList(), 0L, pageNum, pageSize);
    }

    public boolean hasData() {
        return !records.isEmpty();
    }

    public boolean hasNext() {
        return current < pages;
    }

    public boolean hasPrevious() {
        return current > 1;
    }
}