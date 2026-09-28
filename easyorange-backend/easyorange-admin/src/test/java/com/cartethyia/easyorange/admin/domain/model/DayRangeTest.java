package com.cartethyia.easyorange.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DayRange 单元测试")
class DayRangeTest {

    @Test
    @DisplayName("日期串展开成当天首尾时刻：当天算不算在内只在这一处定义")
    void of_coversWholeDay() {
        DayRange range = DayRange.of("2026-05-01", "2026-05-02");

        assertThat(range.start()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(range.end()).isEqualTo(LocalDateTime.of(2026, 5, 2, 23, 59, 59));
    }

    @Test
    @DisplayName("单边为空时只约束另一端")
    void of_allowsOpenEndedRange() {
        DayRange range = DayRange.of("2026-05-01", null);

        assertThat(range.start()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(range.end()).isNull();
    }

    @Test
    @DisplayName("空白按未传处理")
    void of_treatsBlankAsAbsent() {
        DayRange range = DayRange.of("  ", "");

        assertThat(range.start()).isNull();
        assertThat(range.end()).isNull();
    }

    @Test
    @DisplayName("格式非法按不限处理：查询条件写错不该让整个列表 500")
    void of_invalidDateFallsBackToUnbounded() {
        DayRange range = DayRange.of("invalid", "2026-13-01");

        assertThat(range.start()).isNull();
        assertThat(range.end()).isNull();
    }
}
