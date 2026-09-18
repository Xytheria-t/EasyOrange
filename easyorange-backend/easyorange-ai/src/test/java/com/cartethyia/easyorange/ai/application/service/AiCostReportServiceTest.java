package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCostReportService -> 测试")
class AiCostReportServiceTest {

    @Test
    @DisplayName("时间窗 -> 非法值兜底 24h，超上限收敛到 30 天")
    void clampWindow() {
        assertThat(AiCostReportService.clampWindow(0)).isEqualTo(24);
        assertThat(AiCostReportService.clampWindow(-5)).isEqualTo(24);
        assertThat(AiCostReportService.clampWindow(6)).isEqualTo(6);
        assertThat(AiCostReportService.clampWindow(24 * 365)).isEqualTo(AiCostReportService.MAX_WINDOW_HOURS);
    }

    @Test
    @DisplayName("行映射 -> 列名与 DTO 字段对应，totalTokens 为入出之和")
    void rowMapper() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("scope")).thenReturn("PRICING");
        when(rs.getLong("calls")).thenReturn(12L);
        when(rs.getLong("token_input")).thenReturn(8000L);
        when(rs.getLong("token_output")).thenReturn(2000L);
        when(rs.getLong("avg_latency_ms")).thenReturn(1500L);
        when(rs.getLong("failures")).thenReturn(1L);

        AiCostReportRow row = AiCostReportService.rowMapper().mapRow(rs, 0);

        assertThat(row.scope()).isEqualTo("PRICING");
        assertThat(row.calls()).isEqualTo(12);
        assertThat(row.tokenInput()).isEqualTo(8000);
        assertThat(row.tokenOutput()).isEqualTo(2000);
        assertThat(row.totalTokens()).isEqualTo(10_000);
        assertThat(row.avgLatencyMs()).isEqualTo(1500);
        assertThat(row.failures()).isEqualTo(1);
    }
}
