package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.CreditScoreResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {

    private final JdbcTemplate jdbcTemplate;
    private final LlmPort llmPort;

    private static final int BASE_SCORE = 100;
    private static final int SCORE_MIN = 0;
    private static final int SCORE_MAX = 200;

    @Transactional(readOnly = true)
    public CreditScoreResult getCreditScore(String userId) {
        String sql = """
                SELECT credit_score, level, total_trades, completed_trades,
                       cancelled_trades, total_reports, confirmed_reports,
                       review_avg_rating, last_updated
                FROM eo_user_credit
                WHERE user_id = ?
                """;

        org.springframework.jdbc.core.ResultSetExtractor<CreditScoreResult> extractor = rs -> {
            if (rs.next()) {
                return new CreditScoreResult(
                        userId,
                        rs.getInt("credit_score"),
                        rs.getString("level"),
                        rs.getInt("total_trades"),
                        rs.getInt("completed_trades"),
                        rs.getInt("cancelled_trades"),
                        rs.getInt("total_reports"),
                        rs.getInt("confirmed_reports"),
                        rs.getDouble("review_avg_rating"),
                        calculateCompletionRate(
                                rs.getInt("completed_trades"),
                                rs.getInt("total_trades")
                        ),
                        rs.getObject("last_updated", LocalDateTime.class)
                );
            }
            return null;
        };
        return Optional.ofNullable(
                jdbcTemplate.query(sql, extractor, userId)
        ).orElseGet(this::createDefaultCredit);
    }

    @Transactional
    public CreditScoreResult recalculateScore(String userId) {
        var tradeStats = queryTradeStats(userId);
        var reportStats = queryReportStats(userId);
        var reviewStats = queryReviewStats(userId);

        int totalTrades = tradeStats.totalTrades();
        int completedTrades = tradeStats.completedTrades();
        int cancelledTrades = tradeStats.cancelledTrades();
        int totalReports = reportStats.totalReports();
        int confirmedReports = reportStats.confirmedReports();
        Double avgRating = reviewStats.avgRating();

        int tradeBonus = Math.min(completedTrades * 5, 50);
        int cancelPenalty = cancelledTrades * 10;
        int reportPenalty = confirmedReports * 20;
        int ratingBonus = avgRating != null ? (int) Math.round((avgRating - 3.0) * 10) : 0;

        int finalScore = BASE_SCORE + tradeBonus - cancelPenalty - reportPenalty + ratingBonus;
        int creditScore = Math.max(SCORE_MIN, Math.min(SCORE_MAX, finalScore));
        String level = determineLevel(creditScore);
        int completionRate = calculateCompletionRate(completedTrades, totalTrades);

        upsertCredit(userId, creditScore, level, totalTrades, completedTrades,
                cancelledTrades, totalReports, confirmedReports, avgRating);

        log.info("Credit score recalculated for userId={}: score={}, level={}",
                userId, creditScore, level);

        return new CreditScoreResult(
                userId, creditScore, level, totalTrades, completedTrades,
                cancelledTrades, totalReports, confirmedReports,
                avgRating, completionRate, LocalDateTime.now()
        );
    }

    private CreditScoreResult createDefaultCredit() {
        return new CreditScoreResult(null, BASE_SCORE, "NORMAL", 0, 0, 0, 0, 0, null, 100, LocalDateTime.now());
    }

    private int calculateCompletionRate(int completedTrades, int totalTrades) {
        if (totalTrades == 0) {
            return 100;
        }
        return (int) Math.round((double) completedTrades / totalTrades * 100);
    }

    private String determineLevel(int score) {
        if (score >= 160) return "EXCELLENT";
        if (score >= 120) return "GOOD";
        if (score >= 80) return "NORMAL";
        if (score >= 40) return "LOW";
        return "BLACKLIST";
    }

    private TradeStats queryTradeStats(String userId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_trades,
                    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS completed_trades,
                    SUM(CASE WHEN status IN (4, 5) THEN 1 ELSE 0 END) AS cancelled_trades
                FROM eo_order
                WHERE (buyer_id = ? OR seller_id = ?) AND del_flag = 0
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return new TradeStats(
                        rs.getInt("total_trades"),
                        rs.getInt("completed_trades"),
                        rs.getInt("cancelled_trades")
                );
            }
            return new TradeStats(0, 0, 0);
        }, userId, userId);
    }

    private ReportStats queryReportStats(String userId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_reports,
                    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS confirmed_reports
                FROM eo_product_report
                WHERE reporter_id = ? AND del_flag = 0
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return new ReportStats(
                        rs.getInt("total_reports"),
                        rs.getInt("confirmed_reports")
                );
            }
            return new ReportStats(0, 0);
        }, userId);
    }

    private ReviewStats queryReviewStats(String userId) {
        String sql = """
                SELECT COALESCE(AVG(r.rating), 0.0) AS avg_rating
                FROM eo_product_review r
                JOIN eo_order o ON r.order_id = o.id
                WHERE o.seller_id = ? AND r.del_flag = 0 AND o.del_flag = 0
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                double avg = rs.getDouble("avg_rating");
                return new ReviewStats(avg > 0 ? avg : null);
            }
            return new ReviewStats(null);
        }, userId);
    }

    private void upsertCredit(String userId, int creditScore, String level,
                              int totalTrades, int completedTrades, int cancelledTrades,
                              int totalReports, int confirmedReports, Double avgRating) {
        String sql = """
                INSERT INTO eo_user_credit (
                    user_id, credit_score, level, total_trades, completed_trades,
                    cancelled_trades, total_reports, confirmed_reports,
                    review_avg_rating, last_updated, create_time, update_time, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW(), 0)
                AS new
                ON DUPLICATE KEY UPDATE
                    credit_score = new.credit_score,
                    level = new.level,
                    total_trades = new.total_trades,
                    completed_trades = new.completed_trades,
                    cancelled_trades = new.cancelled_trades,
                    total_reports = new.total_reports,
                    confirmed_reports = new.confirmed_reports,
                    review_avg_rating = new.review_avg_rating,
                    last_updated = new.last_updated,
                    update_time = new.update_time,
                    version = version + 1
                """;

        jdbcTemplate.update(sql,
                userId, creditScore, level, totalTrades, completedTrades,
                cancelledTrades, totalReports, confirmedReports,
                avgRating != null ? avgRating : 0.0
        );
    }

    private record TradeStats(int totalTrades, int completedTrades, int cancelledTrades) {}

    private record ReportStats(int totalReports, int confirmedReports) {}

    private record ReviewStats(Double avgRating) {}
}