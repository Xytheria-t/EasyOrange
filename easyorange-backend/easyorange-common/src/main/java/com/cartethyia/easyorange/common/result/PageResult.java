package com.cartethyia.easyorange.common.result;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        int current,
        int size,
        int pages
) {

    public PageResult {
        records = records != null ? records : List.of();
    }

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        int pages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        return new PageResult<>(records, total, pageNum, pageSize, pages);
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(List.of(), 0L, pageNum, pageSize);
    }
}