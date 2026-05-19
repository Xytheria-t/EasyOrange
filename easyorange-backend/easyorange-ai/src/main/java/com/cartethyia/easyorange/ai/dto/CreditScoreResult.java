package com.cartethyia.easyorange.ai.dto;

import java.time.LocalDateTime;

public record CreditScoreResult(
        Long userId,
        int creditScore,
        String level,
        int totalTrades,
        int completedTrades,
        int cancelledTrades,
        int totalReports,
        int confirmedReports,
        Double reviewAvgRating,
        int tradeCompletionRate,
        LocalDateTime lastUpdated
) {}