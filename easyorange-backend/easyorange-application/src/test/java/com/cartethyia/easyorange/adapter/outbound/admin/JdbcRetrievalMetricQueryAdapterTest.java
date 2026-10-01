package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 检索指标回看适配器测试 — 钉住三处最容易悄悄算错的：评测线按前缀过滤、分母取全批用例数
 * （未命中的 rr=0 也进分母）、空批次不做 0/0。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("检索指标回看适配器 -> 测试")
class JdbcRetrievalMetricQueryAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("批次聚合：hit@5 与 MRR 的分母都取全批用例数（未命中的 rr=0 也进分母）")
    void recentRuns_dividesByTotalCaseCount() throws Exception {
        var adapter = new JdbcRetrievalMetricQueryAdapter(jdbcTemplate);
        mockRunsQueryReturning(10, 8, 6.5);

        var runs = adapter.recentRuns(RetrievalEvalLine.KNOWLEDGE, 20);

        assertThat(runs).hasSize(1);
        var run = runs.getFirst();
        assertThat(run.runId()).isEqualTo("run-1");
        assertThat(run.caseCount()).isEqualTo(10);
        assertThat(run.hitCount()).isEqualTo(8);
        assertThat(run.hitRateAt5()).isEqualTo(0.8);
        assertThat(run.mrr()).isEqualTo(0.65);
        assertThat(run.line()).isEqualTo(RetrievalEvalLine.KNOWLEDGE);
    }

    @Test
    @DisplayName("评测线按 case_id 前缀过滤 —— 两条线语料空间不同，混算均值不可解释")
    void recentRuns_filtersByLinePrefix() {
        var adapter = new JdbcRetrievalMetricQueryAdapter(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.recentRuns(RetrievalEvalLine.ASSET, 5);

        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("asset-%"), eq(5));
    }

    @Test
    @DisplayName("空批次 -> 比例记 0，不做 0/0")
    void recentRuns_emptyBatchDoesNotDivideByZero() throws Exception {
        var adapter = new JdbcRetrievalMetricQueryAdapter(jdbcTemplate);
        mockRunsQueryReturning(0, 0, 0);

        var run = adapter.recentRuns(RetrievalEvalLine.KNOWLEDGE, 20).getFirst();

        assertThat(run.hitRateAt5()).isZero();
        assertThat(run.mrr()).isZero();
    }

    @Test
    @DisplayName("用例明细：无数据时返回空页而不是全量明细")
    void pageCases_emptyRunReturnsEmptyPage() {
        var adapter = new JdbcRetrievalMetricQueryAdapter(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);

        var page = adapter.pageCases("run-x", 1, 10);

        assertThat(page.records()).isEmpty();
        assertThat(page.total()).isZero();
        assertThat(page.current()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private void mockRunsQueryReturning(int caseCount, int hitCount, double rrSum) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("run_id")).thenReturn("run-1");
        when(rs.getInt("case_count")).thenReturn(caseCount);
        when(rs.getInt("hit_count")).thenReturn(hitCount);
        when(rs.getDouble("rr_sum")).thenReturn(rrSum);
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });
    }
}
