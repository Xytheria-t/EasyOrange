package com.cartethyia.easyorange.admin.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;

/**
 * 查询时间窗 — 后台列表的时间过滤按「天」传（{@code yyyy-MM-dd}），端口条件要的是时间点。
 * <p>
 * 「当天算不算在内」的边界口径只在这里定一次（起始 00:00:00 / 截止 23:59:59），订单与商品列表共用：
 * 同一句查询在两个接口里含义不同，比多写一遍 try/catch 难查得多。
 * <p>
 * 解析失败按「不限」处理：查询条件写错不该让整个列表 500，但必须留痕。
 */
@Slf4j
public record DayRange(LocalDateTime start, LocalDateTime end) {

    public static DayRange of(String startDate, String endDate) {
        return new DayRange(atStartOfDay(startDate), atEndOfDay(endDate));
    }

    private static LocalDateTime atStartOfDay(String date) {
        LocalDate parsed = parse(date);
        return parsed == null ? null : parsed.atStartOfDay();
    }

    private static LocalDateTime atEndOfDay(String date) {
        LocalDate parsed = parse(date);
        return parsed == null ? null : parsed.atTime(23, 59, 59);
    }

    private static LocalDate parse(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", date);
            return null;
        }
    }
}
