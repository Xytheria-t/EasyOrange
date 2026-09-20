package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * AI 建议采纳率适配器测试。
 * <p>
 * 采纳率是全项目唯一不依赖 LLM 评 LLM 的质量数字，算错了没人能发现 —— 所以比例与除零这两处必须钉住。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 建议采纳率适配器 -> 测试")
class JdbcAiListingAdoptionAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("有样本 -> 逐字段采纳率与全字段采纳率都由 计数/样本数 算出")
    void report_computesFieldRates() throws Exception {
        var adapter = new JdbcAiListingAdoptionAdapter(jdbcTemplate);
        mockQueryReturning(new Counts(10L, 7L, 4L, 6L, 3L, 9L, 8L, 2L, 8L, 1L, 12.5));

        var report = adapter.report();

        assertThat(report.samples()).isEqualTo(10);
        assertThat(report.fullyAdopted()).isEqualTo(2);
        assertThat(report.fullAdoptionRate()).isEqualTo(0.2);
        assertThat(report.within10Percent()).isEqualTo(8);
        assertThat(report.beyond30Percent()).isEqualTo(1);
        assertThat(report.avgPriceDeviationPercent()).isEqualTo(12.5);

        assertThat(report.fields())
                .extracting(AiListingAdoptionReport.FieldAdoption::field)
                .containsExactly("title", "description", "price", "categoryName", "conditionLevel", "location");
        assertThat(report.fields())
                .extracting(AiListingAdoptionReport.FieldAdoption::adopted)
                .containsExactly(7L, 4L, 6L, 3L, 9L, 8L);
        assertThat(report.fields())
                .extracting(AiListingAdoptionReport.FieldAdoption::adoptionRate)
                .containsExactly(0.7, 0.4, 0.6, 0.3, 0.9, 0.8);
    }

    @Test
    @DisplayName("一个样本都没有 -> 所有比例记 0，不做 0/0")
    void report_noSamples() throws Exception {
        var adapter = new JdbcAiListingAdoptionAdapter(jdbcTemplate);
        mockQueryReturning(new Counts(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0d));

        var report = adapter.report();

        assertThat(report.samples()).isZero();
        assertThat(report.fullAdoptionRate()).isZero();
        assertThat(report.fields())
                .extracting(AiListingAdoptionReport.FieldAdoption::adoptionRate)
                .containsOnly(0d);
    }

    /** 让 mock 真正走一遍适配器里的 RowMapper（只 mock 掉 JDBC，不 mock 掉被验证的映射逻辑）。 */
    @SuppressWarnings("unchecked")
    private void mockQueryReturning(Counts c) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("samples")).thenReturn(c.samples());
        when(rs.getLong("title_adopted")).thenReturn(c.title());
        when(rs.getLong("description_adopted")).thenReturn(c.description());
        when(rs.getLong("price_adopted")).thenReturn(c.price());
        when(rs.getLong("category_adopted")).thenReturn(c.category());
        when(rs.getLong("condition_adopted")).thenReturn(c.condition());
        when(rs.getLong("location_adopted")).thenReturn(c.location());
        when(rs.getLong("fully_adopted")).thenReturn(c.fully());
        when(rs.getLong("within10")).thenReturn(c.within10());
        when(rs.getLong("beyond30")).thenReturn(c.beyond30());
        when(rs.getDouble("avg_deviation")).thenReturn(c.avgDeviation());
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(1)).mapRow(rs, 0));
    }

    /** 一条 SQL 结果行的全部计数（顺序与 SQL 的 SELECT 列表对齐，避免长参数列表错位）。 */
    private record Counts(
            long samples,
            long title,
            long description,
            long price,
            long category,
            long condition,
            long location,
            long fully,
            long within10,
            long beyond30,
            double avgDeviation) {}
}
