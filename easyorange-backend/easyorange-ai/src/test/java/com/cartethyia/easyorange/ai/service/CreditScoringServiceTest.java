package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.CreditScoreResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditScoringService 测试")
class CreditScoringServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LlmPort llmPort;

    private CreditScoringService service;

    @BeforeEach
    void setUp() {
        service = new CreditScoringService(jdbcTemplate, llmPort);
    }

    @Nested
    @DisplayName("getCreditScore")
    class GetCreditScoreTests {

        @Test
        @DisplayName("用户存在时返回信用分")
        void getCreditScore_userExists() throws Exception {
            String userId = "1";
            ResultSet rs = mock(ResultSet.class);
            when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(userId)))
                    .thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(rs));

            when(rs.next()).thenReturn(true);
            when(rs.getInt("credit_score")).thenReturn(120);
            when(rs.getString("level")).thenReturn("GOOD");
            when(rs.getInt("total_trades")).thenReturn(10);
            when(rs.getInt("completed_trades")).thenReturn(8);
            when(rs.getInt("cancelled_trades")).thenReturn(1);
            when(rs.getInt("total_reports")).thenReturn(2);
            when(rs.getInt("confirmed_reports")).thenReturn(0);
            when(rs.getDouble("review_avg_rating")).thenReturn(4.5);
            when(rs.getObject("last_updated", LocalDateTime.class)).thenReturn(LocalDateTime.now());

            CreditScoreResult result = service.getCreditScore(userId);

            assertThat(result.creditScore()).isEqualTo(120);
            assertThat(result.level()).isEqualTo("GOOD");
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.totalTrades()).isEqualTo(10);
            assertThat(result.completedTrades()).isEqualTo(8);
        }

        @Test
        @DisplayName("用户不存在时返回默认信用分")
        void getCreditScore_userNotExists() {
            String userId = "99";

            when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(userId)))
                    .thenReturn(null);

            CreditScoreResult result = service.getCreditScore(userId);

            assertThat(result.userId()).isNull();
            assertThat(result.creditScore()).isEqualTo(100);
            assertThat(result.level()).isEqualTo("NORMAL");
            assertThat(result.totalTrades()).isZero();
            assertThat(result.tradeCompletionRate()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("recalculateScore")
    class RecalculateScoreTests {

        private final String userId = "1";

        @Test
        @DisplayName("正常交易数据 — 计算正确")
        void recalculateScore_normal() throws Exception {
            mockQuery("eo_order", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_trades")).thenReturn(10);
                when(rs.getInt("completed_trades")).thenReturn(8);
                when(rs.getInt("cancelled_trades")).thenReturn(1);
            });
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(2);
                when(rs.getInt("confirmed_reports")).thenReturn(0);
            });
            mockQuery("eo_product_review", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getDouble("avg_rating")).thenReturn(4.5);
            });

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isEqualTo(145);
            assertThat(result.level()).isEqualTo("GOOD");
            assertThat(result.totalTrades()).isEqualTo(10);
            assertThat(result.completedTrades()).isEqualTo(8);
            assertThat(result.tradeCompletionRate()).isEqualTo(80);
        }

        @Test
        @DisplayName("交易奖励上限 50")
        void recalculateScore_tradeBonusCap() throws Exception {
            mockQuery("eo_order", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_trades")).thenReturn(20);
                when(rs.getInt("completed_trades")).thenReturn(20);
                when(rs.getInt("cancelled_trades")).thenReturn(0);
            });
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(0);
                when(rs.getInt("confirmed_reports")).thenReturn(0);
            });
            mockQuery("eo_product_review", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getDouble("avg_rating")).thenReturn(5.0);
            });

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isEqualTo(170);
            assertThat(result.level()).isEqualTo("EXCELLENT");
        }

        @Test
        @DisplayName("取消交易和举报 — 扣分正确")
        void recalculateScore_penalties() throws Exception {
            mockQuery("eo_order", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_trades")).thenReturn(5);
                when(rs.getInt("completed_trades")).thenReturn(1);
                when(rs.getInt("cancelled_trades")).thenReturn(4);
            });
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(3);
                when(rs.getInt("confirmed_reports")).thenReturn(1);
            });
            mockEmptyReviewQuery();

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isEqualTo(45);
            assertThat(result.level()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("分数超出下限时归为 BLACKLIST")
        void recalculateScore_belowMinScore() throws Exception {
            mockQuery("eo_order", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_trades")).thenReturn(0);
                when(rs.getInt("completed_trades")).thenReturn(0);
                when(rs.getInt("cancelled_trades")).thenReturn(10);
            });
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(10);
                when(rs.getInt("confirmed_reports")).thenReturn(10);
            });
            mockQuery("eo_product_review", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getDouble("avg_rating")).thenReturn(1.0);
            });

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isZero();
            assertThat(result.level()).isEqualTo("BLACKLIST");
        }

        @Test
        @DisplayName("综合评价为空时不计入加分")
        void recalculateScore_noReviewData() throws Exception {
            mockQuery("eo_order", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_trades")).thenReturn(5);
                when(rs.getInt("completed_trades")).thenReturn(5);
                when(rs.getInt("cancelled_trades")).thenReturn(0);
            });
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(0);
                when(rs.getInt("confirmed_reports")).thenReturn(0);
            });
            mockQuery("eo_product_review", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getDouble("avg_rating")).thenReturn(0.0);
            });

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isEqualTo(125);
            assertThat(result.level()).isEqualTo("GOOD");
        }

        @Test
        @DisplayName("没有交易数据 — queryTradeStats 返回空")
        void recalculateScore_noTrades() throws Exception {
            mockQuery("eo_order", rs -> when(rs.next()).thenReturn(false));
            mockQuery("eo_product_report", rs -> {
                when(rs.next()).thenReturn(true);
                when(rs.getInt("total_reports")).thenReturn(0);
                when(rs.getInt("confirmed_reports")).thenReturn(0);
            });
            mockEmptyReviewQuery();

            CreditScoreResult result = service.recalculateScore(userId);

            assertThat(result.creditScore()).isEqualTo(100);
            assertThat(result.level()).isEqualTo("NORMAL");
        }

        private void mockQuery(String table, ThrowingConsumer<ResultSet> consumer) throws Exception {
            ResultSet rs = mock(ResultSet.class);
            consumer.accept(rs);
            boolean isTradeQuery = table.equals("eo_order");
            if (isTradeQuery) {
                lenient().when(jdbcTemplate.query(
                        argThat(sql -> sql != null && sql.contains(table)),
                        any(ResultSetExtractor.class), anyString(), anyString()
                )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(rs));
            } else {
                lenient().when(jdbcTemplate.query(
                        argThat(sql -> sql != null && sql.contains(table)),
                        any(ResultSetExtractor.class), anyString()
                )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(rs));
            }
        }

        private void mockEmptyReviewQuery() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            lenient().when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("eo_product_review")),
                    any(ResultSetExtractor.class), anyString()
            )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(rs));
        }
    }

    @Nested
    @DisplayName("等级判定边界值")
    class DetermineLevelTests {

        @Test
        @DisplayName("score 160+ → EXCELLENT")
        void level_excellent() throws Exception {
            CreditScoreResult result = runWithData(
                    12, 0, 0, 5.0);
            assertThat(result.creditScore()).isEqualTo(170);
            assertThat(result.level()).isEqualTo("EXCELLENT");
        }

        @Test
        @DisplayName("score 120-159 → GOOD")
        void level_good() throws Exception {
            CreditScoreResult result = runWithData(
                    4, 0, 0, null);
            assertThat(result.creditScore()).isEqualTo(120);
            assertThat(result.level()).isEqualTo("GOOD");
        }

        @Test
        @DisplayName("score 80-119 → NORMAL（边界 80）")
        void level_normal_boundary() throws Exception {
            CreditScoreResult result = runWithData(
                    0, 0, 1, null);
            assertThat(result.creditScore()).isEqualTo(80);
            assertThat(result.level()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("score 40-79 → LOW（边界 50）")
        void level_low_boundary() throws Exception {
            CreditScoreResult result = runWithData(
                    0, 3, 1, null);
            assertThat(result.creditScore()).isEqualTo(50);
            assertThat(result.level()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("score < 40 → BLACKLIST（边界 10）")
        void level_blacklist_boundary() throws Exception {
            CreditScoreResult result = runWithData(
                    0, 3, 3, null);
            assertThat(result.creditScore()).isEqualTo(10);
            assertThat(result.level()).isEqualTo("BLACKLIST");
        }

        private CreditScoreResult runWithData(int completedTrades, int cancelledTrades,
                                              int confirmedReports, Double avgRating) throws Exception {
            ResultSet tradeRs = mock(ResultSet.class);
            when(tradeRs.next()).thenReturn(true);
            when(tradeRs.getInt("total_trades")).thenReturn(completedTrades + cancelledTrades);
            when(tradeRs.getInt("completed_trades")).thenReturn(completedTrades);
            when(tradeRs.getInt("cancelled_trades")).thenReturn(cancelledTrades);

            ResultSet reportRs = mock(ResultSet.class);
            when(reportRs.next()).thenReturn(true);
            when(reportRs.getInt("total_reports")).thenReturn(confirmedReports);
            when(reportRs.getInt("confirmed_reports")).thenReturn(confirmedReports);

            ResultSet reviewRs = mock(ResultSet.class);
            if (avgRating != null) {
                when(reviewRs.next()).thenReturn(true);
                when(reviewRs.getDouble("avg_rating")).thenReturn(avgRating);
            } else {
                when(reviewRs.next()).thenReturn(false);
            }

            lenient().when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("eo_order")),
                    any(ResultSetExtractor.class), anyString(), anyString()
            )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(tradeRs));

            lenient().when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("eo_product_report")),
                    any(ResultSetExtractor.class), anyString()
            )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(reportRs));

            lenient().when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("eo_product_review")),
                    any(ResultSetExtractor.class), anyString()
            )).thenAnswer(inv -> ((ResultSetExtractor<?>) inv.getArgument(1)).extractData(reviewRs));

            lenient().when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(1);

            return service.recalculateScore("1");
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}