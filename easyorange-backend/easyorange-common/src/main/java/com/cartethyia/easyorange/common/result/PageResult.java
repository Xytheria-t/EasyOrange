package com.cartethyia.easyorange.common.result;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> records, long total, int current, int size, int pages) {

    public PageResult {
        records = records != null ? records : List.of();
    }

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize, calcPages(total, pageSize));
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(List.of(), 0L, pageNum, pageSize);
    }

    /**
     * 只换页内元素类型，分页元数据原样带过 —— 各模块查询侧反复手写
     * 「map 记录 + 重新组装 PageResult」，漏抄 total 的风险高于收益。
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = records.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, total, current, size, pages);
    }

    private static int calcPages(long total, int size) {
        return size > 0 ? (int) ((total + size - 1) / size) : 0;
    }
}
