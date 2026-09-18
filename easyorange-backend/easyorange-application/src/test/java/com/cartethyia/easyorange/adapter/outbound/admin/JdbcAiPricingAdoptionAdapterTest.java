package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * AI 建议价采纳率适配器测试。
 * <p>
 * 采纳率是全项目唯一不依赖 LLM 评 LLM 的质量数字，算错了没人能发现 —— 所以比例与除零这两处必须钉住。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 建议价采纳率适配器 -> 测试")
class JdbcAiPricingAdoptionAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("有样本 -> 采纳率 = 采纳数 / 样本数，其余字段原样透传")
    void report_computesAdoptionRate() throws Exception {
        var adapter = new JdbcAiPricingAdoptionAdapter(jdbcTemplate);
        mockQueryReturning(10L, 6L, 8L, 1L, 12.5);

        var report = adapter.report();

        assertThat(report.samples()).isEqualTo(10);
        assertThat(report.adopted()).isEqualTo(6);
        assertThat(report.adoptionRate()).isEqualTo(0.6);
        assertThat(report.within10Percent()).isEqualTo(8);
        assertThat(report.beyond30Percent()).isEqualTo(1);
        assertThat(report.avgDeviationPercent()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("一个样本都没有 -> 采纳率记 0，不做 0/0")
    void report_noSamples() throws Exception {
        var adapter = new JdbcAiPricingAdoptionAdapter(jdbcTemplate);
        mockQueryReturning(0L, 0L, 0L, 0L, 0d);

        var report = adapter.report();

        assertThat(report.samples()).isZero();
        assertThat(report.adoptionRate()).isZero();
    }

    /** 让 mock 真正走一遍适配器里的 RowMapper（只 mock 掉 JDBC，不 mock 掉被验证的映射逻辑）。 */
    @SuppressWarnings("unchecked")
    private void mockQueryReturning(long samples, long adopted, long within10, long beyond30, double avgDeviation)
            throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("samples")).thenReturn(samples);
        when(rs.getLong("adopted")).thenReturn(adopted);
        when(rs.getLong("within10")).thenReturn(within10);
        when(rs.getLong("beyond30")).thenReturn(beyond30);
        when(rs.getDouble("avg_deviation")).thenReturn(avgDeviation);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(1)).mapRow(rs, 0));
    }
}
