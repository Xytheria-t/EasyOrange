package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.dto.CreditScoreResult;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcCreditScoreFetcher 测试")
class JdbcCreditScoreFetcherTest {

    @Mock
    private CreditScoringService creditScoringService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcCreditScoreFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new JdbcCreditScoreFetcher(creditScoringService, jdbcTemplate);
    }

    @Nested
    @DisplayName("fetchCreditScores")
    class FetchCreditScoresTests {

        @Test
        @DisplayName("空集合直接返回空 Map")
        void fetchCreditScores_empty() {
            assertThat(fetcher.fetchCreditScores(List.of())).isEmpty();
        }

        @Test
        @DisplayName("null 集合直接返回空 Map")
        void fetchCreditScores_null() {
            assertThat(fetcher.fetchCreditScores(null)).isEmpty();
        }

        @Test
        @DisplayName("批量 JDBC 查询成功")
        void fetchCreditScores_batchSuccess() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            doAnswer(inv -> {
                        RowCallbackHandler rch = inv.getArgument(1);
                        when(rs.getString("user_id")).thenReturn("u1", "u2");
                        when(rs.getInt("credit_score")).thenReturn(120, 95);
                        rch.processRow(rs);
                        rch.processRow(rs);
                        return null;
                    })
                    .when(jdbcTemplate)
                    .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

            Map<String, Integer> result = fetcher.fetchCreditScores(Set.of("u1", "u2"));

            assertThat(result).containsEntry("u1", 120).containsEntry("u2", 95);
            verify(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
            verify(creditScoringService, never()).getCreditScore(anyString());
        }

        @Test
        @DisplayName("批量查询失败降级为逐个查询")
        void fetchCreditScores_fallback() {
            doThrow(new RuntimeException("db down"))
                    .when(jdbcTemplate)
                    .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
            when(creditScoringService.getCreditScore("u1")).thenReturn(creditResult("u1", 88));

            Map<String, Integer> result = fetcher.fetchCreditScores(Set.of("u1"));

            assertThat(result).containsEntry("u1", 88);
            verify(creditScoringService).getCreditScore("u1");
        }

        @Test
        @DisplayName("降级时无信用分的卖家被跳过")
        void fetchCreditScores_fallbackSkipsNull() {
            doThrow(new RuntimeException("db down"))
                    .when(jdbcTemplate)
                    .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
            when(creditScoringService.getCreditScore("u1")).thenReturn(null);

            Map<String, Integer> result = fetcher.fetchCreditScores(Set.of("u1"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("降级时查询抛异常被吞掉")
        void fetchCreditScores_fallbackIgnoresErrors() {
            doThrow(new RuntimeException("db down"))
                    .when(jdbcTemplate)
                    .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
            when(creditScoringService.getCreditScore("u1")).thenThrow(new RuntimeException("lookup failed"));

            Map<String, Integer> result = fetcher.fetchCreditScores(Set.of("u1"));

            assertThat(result).isEmpty();
        }

        private CreditScoreResult creditResult(String userId, int score) {
            return new CreditScoreResult(userId, score, "GOOD", 5, 4, 0, 0, 0, 4.5, 80, null);
        }
    }
}
