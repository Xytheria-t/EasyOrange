package com.cartethyia.easyorange.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtils 单元测试
 */
@DisplayName("DateUtils 单元测试")
class DateUtilsTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Nested
    @DisplayName("格式化")
    class FormatTests {

        @Test
        @DisplayName("LocalDateTime 转默认格式字符串")
        void format_localDateTime_defaultPattern() {
            LocalDateTime dt = LocalDateTime.of(2026, 4, 1, 12, 30, 45);
            assertEquals("2026-04-01 12:30:45", DateUtils.format(dt));
        }

        @Test
        @DisplayName("LocalDateTime 转自定义格式字符串")
        void format_localDateTime_customPattern() {
            LocalDateTime dt = LocalDateTime.of(2026, 4, 1, 12, 30, 45);
            assertEquals("20260401123045", DateUtils.format(dt, "yyyyMMddHHmmss"));
        }

        @Test
        @DisplayName("LocalDate 转默认格式字符串")
        void format_localDate_defaultPattern() {
            LocalDate date = LocalDate.of(2026, 4, 1);
            assertEquals("2026-04-01", DateUtils.format(date));
        }

        @Test
        @DisplayName("null 输入返回 null")
        void format_nullInput_returnsNull() {
            assertNull(DateUtils.format((LocalDateTime) null));
            assertNull(DateUtils.format((LocalDate) null));
        }
    }

    @Nested
    @DisplayName("解析")
    class ParseTests {

        @Test
        @DisplayName("字符串转 LocalDateTime")
        void parse_defaultPattern() {
            LocalDateTime dt = DateUtils.parse("2026-04-01 12:30:45");
            assertNotNull(dt);
            assertEquals(2026, dt.getYear());
            assertEquals(4, dt.getMonthValue());
            assertEquals(1, dt.getDayOfMonth());
            assertEquals(12, dt.getHour());
        }

        @Test
        @DisplayName("字符串转 LocalDateTime 自定义格式")
        void parse_customPattern() {
            LocalDateTime dt = DateUtils.parse("20260401123045", "yyyyMMddHHmmss");
            assertNotNull(dt);
            assertEquals(2026, dt.getYear());
        }

        @Test
        @DisplayName("null 或空白字符串返回 null")
        void parse_nullOrBlank_returnsNull() {
            assertNull(DateUtils.parse(null));
            assertNull(DateUtils.parse(""));
            assertNull(DateUtils.parse("   "));
        }
    }

    @Nested
    @DisplayName("转换")
    class ConvertTests {

        @Test
        @DisplayName("Date 转 LocalDateTime")
        void toLocalDateTime_fromDate() {
            java.util.Date date = new java.util.Date(0);
            LocalDateTime dt = DateUtils.toLocalDateTime(date);
            assertNotNull(dt);
        }

        @Test
        @DisplayName("LocalDateTime 转 Date")
        void toDate_fromLocalDateTime() {
            LocalDateTime dt = LocalDateTime.of(2026, 4, 1, 0, 0);
            java.util.Date date = DateUtils.toDate(dt);
            assertNotNull(date);
        }

        @Test
        @DisplayName("null 输入返回 null")
        void convert_nullInput_returnsNull() {
            assertNull(DateUtils.toLocalDateTime(null));
            assertNull(DateUtils.toDate((LocalDateTime) null));
            assertNull(DateUtils.toDate((LocalDate) null));
        }
    }

    @Nested
    @DisplayName("now / today")
    class NowTests {

        @Test
        @DisplayName("获取当前时间")
        void now_returnsCurrentTime() {
            LocalDateTime now = DateUtils.now();
            assertNotNull(now);
        }

        @Test
        @DisplayName("获取当前日期")
        void today_returnsCurrentDate() {
            LocalDate today = DateUtils.today();
            assertNotNull(today);
        }

        @Test
        @DisplayName("获取当前时间戳")
        void currentMillis_returnsPositiveValue() {
            long millis = DateUtils.currentMillis();
            assertTrue(millis > 0);
        }
    }

    @Nested
    @DisplayName("业务方法")
    class BusinessMethodTests {

        @Test
        @DisplayName("判断是否过期")
        void isExpired() {
            LocalDateTime past = LocalDateTime.now().minusDays(1);
            LocalDateTime future = LocalDateTime.now().plusDays(1);
            assertTrue(DateUtils.isExpired(past));
            assertFalse(DateUtils.isExpired(future));
        }

        @Test
        @DisplayName("获取当天开始/结束时间")
        void getDayStartEnd() {
            LocalDateTime start = DateUtils.getDayStart();
            LocalDateTime end = DateUtils.getDayEnd();
            assertEquals(0, start.getHour());
            assertEquals(23, end.getHour());
        }

        @Test
        @DisplayName("获取指定日期开始/结束时间")
        void getDayStartEnd_withDate() {
            LocalDate date = LocalDate.of(2026, 4, 1);
            LocalDateTime start = DateUtils.getDayStart(date);
            LocalDateTime end = DateUtils.getDayEnd(date);
            assertEquals(2026, start.getYear());
            assertEquals(4, start.getMonthValue());
            assertEquals(1, start.getDayOfMonth());
            assertEquals(23, end.getHour());
        }

        @Test
        @DisplayName("获取星期几")
        void getDayOfWeek() {
            LocalDate monday = LocalDate.of(2026, 3, 30);
            assertEquals(1, DateUtils.getDayOfWeek(monday));
        }

        @Test
        @DisplayName("判断是否为今天")
        void isToday() {
            assertTrue(DateUtils.isToday(LocalDate.now()));
            assertFalse(DateUtils.isToday(LocalDate.now().minusDays(1)));
        }

        @Test
        @DisplayName("判断日期是否在区间内")
        void isBetween() {
            LocalDate start = LocalDate.of(2026, 4, 1);
            LocalDate end = LocalDate.of(2026, 4, 30);
            assertTrue(DateUtils.isBetween(LocalDate.of(2026, 4, 15), start, end));
            assertFalse(DateUtils.isBetween(LocalDate.of(2026, 5, 1), start, end));
        }

        @Test
        @DisplayName("获取月份第一天")
        void getMonthStart() {
            LocalDateTime start = DateUtils.getMonthStart(LocalDate.of(2026, 4, 15));
            assertEquals(1, start.getDayOfMonth());
        }

        @Test
        @DisplayName("获取月份最后一天")
        void getMonthEnd() {
            LocalDateTime end = DateUtils.getMonthEnd(LocalDate.of(2026, 4, 15));
            assertEquals(30, end.getDayOfMonth());
        }

        @Test
        @DisplayName("获取周周一")
        void getWeekStart() {
            LocalDateTime start = DateUtils.getWeekStart(LocalDate.of(2026, 4, 1));
            assertEquals(1, start.getDayOfWeek().getValue());
        }

        @Test
        @DisplayName("获取周周日")
        void getWeekEnd() {
            LocalDateTime end = DateUtils.getWeekEnd(LocalDate.of(2026, 4, 1));
            assertEquals(7, end.getDayOfWeek().getValue());
        }

        @Test
        @DisplayName("计算年龄")
        void getAge() {
            LocalDate birthDate = LocalDate.of(1990, 1, 1);
            int age = DateUtils.getAge(birthDate, 0);
            assertTrue(age > 0);
        }

        @Test
        @DisplayName("出生日期为 null 返回默认年龄")
        void getAge_nullBirthDate_returnsDefault() {
            assertEquals(18, DateUtils.getAge(null, 18));
        }
    }

    @Nested
    @DisplayName("新增方法")
    class NewMethodTests {

        @Test
        @DisplayName("计算天数差")
        void daysBetween() {
            LocalDate start = LocalDate.of(2026, 4, 1);
            LocalDate end = LocalDate.of(2026, 4, 10);
            assertEquals(9, DateUtils.daysBetween(start, end));
        }

        @Test
        @DisplayName("计算月数差")
        void monthsBetween() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 4, 1);
            assertEquals(3, DateUtils.monthsBetween(start, end));
        }

        @Test
        @DisplayName("增加天数")
        void addDays() {
            LocalDate date = LocalDate.of(2026, 4, 1);
            assertEquals(LocalDate.of(2026, 4, 10), DateUtils.addDays(date, 9));
        }

        @Test
        @DisplayName("增加月数")
        void addMonths() {
            LocalDate date = LocalDate.of(2026, 4, 1);
            assertEquals(LocalDate.of(2026, 7, 1), DateUtils.addMonths(date, 3));
        }

        @Test
        @DisplayName("判断是否为周末")
        void isWeekend() {
            LocalDate saturday = LocalDate.of(2026, 4, 4);
            LocalDate monday = LocalDate.of(2026, 4, 6);
            assertTrue(DateUtils.isWeekend(saturday));
            assertFalse(DateUtils.isWeekend(monday));
        }

        @Test
        @DisplayName("判断是否为工作日")
        void isWeekday() {
            LocalDate monday = LocalDate.of(2026, 4, 6);
            LocalDate sunday = LocalDate.of(2026, 4, 5);
            assertTrue(DateUtils.isWeekday(monday));
            assertFalse(DateUtils.isWeekday(sunday));
        }
    }

    @Nested
    @DisplayName("Clock 支持")
    class ClockTests {

        @Test
        @DisplayName("设置固定 Clock")
        void setClock_fixedClock() {
            Instant fixedInstant = Instant.parse("2026-06-15T10:00:00Z");
            Clock fixedClock = Clock.fixed(fixedInstant, SHANGHAI);

            DateUtils.setClock(fixedClock);
            try {
                LocalDate today = DateUtils.todayWithClock();
                assertEquals(2026, today.getYear());
                assertEquals(6, today.getMonthValue());
                assertEquals(15, today.getDayOfMonth());
            } finally {
                DateUtils.resetClock();
            }
        }

        @Test
        @DisplayName("重置 Clock 恢复系统时间")
        void resetClock() {
            DateUtils.setClock(Clock.fixed(Instant.EPOCH, SHANGHAI));
            DateUtils.resetClock();
            LocalDate today = DateUtils.todayWithClock();
            assertNotEquals(1970, today.getYear());
        }
    }
}
