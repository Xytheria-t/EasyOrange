package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCostReportQueryRepository (成本聚合) -> 测试")
class AiCostReportQueryRepositoryTest {

    @Test
    @DisplayName("行映射 -> 列名与 DTO 字段对应，totalTokens 为入出之和")
    void rowMapper() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("scope")).thenReturn("CHAT");
        when(rs.getLong("calls")).thenReturn(12L);
        when(rs.getLong("token_input")).thenReturn(8000L);
        when(rs.getLong("token_output")).thenReturn(2000L);
        when(rs.getLong("avg_latency_ms")).thenReturn(1500L);
        when(rs.getLong("failures")).thenReturn(1L);

        AiCostReportRow row = AiCostReportQueryRepository.rowMapper().mapRow(rs, 0);

        assertThat(row.scope()).isEqualTo("CHAT");
        assertThat(row.calls()).isEqualTo(12);
        assertThat(row.tokenInput()).isEqualTo(8000);
        assertThat(row.tokenOutput()).isEqualTo(2000);
        assertThat(row.totalTokens()).isEqualTo(10_000);
        assertThat(row.avgLatencyMs()).isEqualTo(1500);
        assertThat(row.failures()).isEqualTo(1);
    }
}
