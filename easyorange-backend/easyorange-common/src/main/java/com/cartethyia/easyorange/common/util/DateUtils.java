package com.cartethyia.easyorange.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日期时间工具类
 * <p>
 * 基于 Java 8+ java.time API 封装，提供常用的日期格式化、转换、计算等操作。
 * 所有时区相关操作默认使用 {@link #DEFAULT_ZONE}（Asia/Shanghai）。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>仅封装有业务价值的方法，简单场景请直接使用 {@code LocalDateTime.plusDays()} 等原生 API</li>
 *   <li>所有格式化器线程安全且预编译</li>
 * </ul>
 *
 * @author cartethyia
 */
public final class DateUtils {

    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_COMPACT_PATTERN = "yyyyMMddHHmmss";

    /**
     * 默认时区：Asia/Shanghai
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    // ==================== 预编译格式化器（线程安全）====================

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final DateTimeFormatter DATETIME_COMPACT_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_COMPACT_PATTERN);

    /**
     * 动态 pattern 缓存，避免重复创建 DateTimeFormatter
     */
    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    // ==================== 格式化 ====================

    /**
     * LocalDateTime 转字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * LocalDateTime 转字符串（指定格式）
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(getFormatter(pattern));
    }

    /**
     * LocalDate 转字符串（默认格式：yyyy-MM-dd）
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * LocalDate 转字符串（指定格式）
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(getFormatter(pattern));
    }

    /**
     * 字符串转 LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 字符串转 LocalDateTime（指定格式）
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, getFormatter(pattern));
    }

    // ==================== 转换 ====================

    /**
     * Date 转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * LocalDateTime 转 Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * LocalDate 转 Date（当天 00:00:00）
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return toDate(date.atStartOfDay());
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long currentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前 LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    // ==================== 业务方法 ====================

    /**
     * 判断是否已过期（与当前时间比较）
     */
    public static boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && LocalDateTime.now(DEFAULT_ZONE).isAfter(expireTime);
    }

    /**
     * 获取当天的开始时间（00:00:00）
     */
    public static LocalDateTime getDayStart() {
        return LocalDate.now(DEFAULT_ZONE).atStartOfDay();
    }

    /**
     * 获取当天的结束时间（23:59:59.999999999）
     */
    public static LocalDateTime getDayEnd() {
        return LocalDate.now(DEFAULT_ZONE).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期的开始时间
     */
    public static LocalDateTime getDayStart(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间
     */
    public static LocalDateTime getDayEnd(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期是星期几（1=周一, 7=周日）
     */
    public static int getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now(DEFAULT_ZONE));
    }

    /**
     * 判断日期是否在指定区间内（含边界）
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 获取指定日期所在月的第一天
     */
    public static LocalDateTime getMonthStart(LocalDate date) {
        return date.withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取指定日期所在月的最后一天
     */
    public static LocalDateTime getMonthEnd(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在周的周一（00:00:00）
     */
    public static LocalDateTime getWeekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY).atStartOfDay();
    }

    /**
     * 获取指定日期所在周的周日（23:59:59.999999999）
     */
    public static LocalDateTime getWeekEnd(LocalDate date) {
        return date.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
    }

    /**
     * 根据出生日期计算年龄
     *
     * @param birthDate 出生日期
     * @param defaultAge 出生日期为 null 时的默认年龄
     * @return 年龄
     */
    public static int getAge(LocalDate birthDate, int defaultAge) {
        if (birthDate == null) {
            return defaultAge;
        }
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now(DEFAULT_ZONE));
    }

    // ==================== 时间差计算 ====================

    /**
     * 计算两个日期之间的天数差
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期之间的月数差
     */
    public static long monthsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(start, end);
    }

    // ==================== 日期加减 ====================

    /**
     * 增加/减少天数
     */
    public static LocalDate addDays(LocalDate date, long days) {
        if (date == null) return null;
        return date.plusDays(days);
    }

    /**
     * 增加/减少月数
     */
    public static LocalDate addMonths(LocalDate date, long months) {
        if (date == null) return null;
        return date.plusMonths(months);
    }

    /**
     * 增加/减少天数
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) return null;
        return dateTime.plusDays(days);
    }

    /**
     * 增加/减少月数
     */
    public static LocalDateTime addMonths(LocalDateTime dateTime, long months) {
        if (dateTime == null) return null;
        return dateTime.plusMonths(months);
    }

    /**
     * 判断是否为周末
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为工作日
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }

    // ==================== Clock 支持（测试友好）====================

    private static volatile Clock clock = Clock.system(DEFAULT_ZONE);

    /**
     * 设置自定义 Clock（用于单元测试 mock 时间）
     *
     * <pre>{@code
     * // 测试中固定时间
     * Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
     * DateUtils.setClock(fixedClock);
     * }</pre>
     */
    public static void setClock(Clock customClock) {
        clock = customClock;
    }

    /**
     * 重置为系统默认 Clock
     */
    public static void resetClock() {
        clock = Clock.system(DEFAULT_ZONE);
    }

    /**
     * 获取当前 LocalDateTime（使用配置的 Clock）
     */
    public static LocalDateTime nowWithClock() {
        return LocalDateTime.now(clock);
    }

    /**
     * 获取当前 LocalDate（使用配置的 Clock）
     */
    public static LocalDate todayWithClock() {
        return LocalDate.now(clock);
    }
}