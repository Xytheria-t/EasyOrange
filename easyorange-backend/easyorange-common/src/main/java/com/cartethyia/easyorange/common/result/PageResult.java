package com.cartethyia.easyorange.common.result;

import java.util.List;

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
                records != null ? records : List.of(),
                total,
                pageNum,
                pageSize,
                pages
        );
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(List.of(), 0L, pageNum, pageSize);
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
