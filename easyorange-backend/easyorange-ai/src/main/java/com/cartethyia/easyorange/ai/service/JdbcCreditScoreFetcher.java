package com.cartethyia.easyorange.ai.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcCreditScoreFetcher implements CreditScoreFetcher {

    private final CreditScoringService creditScoringService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Integer> fetchCreditScores(Collection<String> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> result = new ConcurrentHashMap<>(sellerIds.size());
        String sql = "SELECT user_id, credit_score FROM eo_user_credit WHERE user_id IN ("
                + String.join(",", Collections.nCopies(sellerIds.size(), "?")) + ")";

        try {
            jdbcTemplate.query(
                    sql,
                    (RowCallbackHandler) rs -> result.put(rs.getString("user_id"), rs.getInt("credit_score")),
                    (Object[]) sellerIds.toArray(new String[0]));
        } catch (Exception e) {
            log.warn("Batch credit score fetch failed, falling back to individual lookup", e);
            return fallbackIndividualLookup(sellerIds);
        }
        return result;
    }

    private Map<String, Integer> fallbackIndividualLookup(Collection<String> sellerIds) {
        Map<String, Integer> result = new HashMap<>();
        for (String sellerId : sellerIds) {
            try {
                var creditResult = creditScoringService.getCreditScore(sellerId);
                if (creditResult != null) {
                    result.put(sellerId, creditResult.creditScore());
                }
            } catch (Exception e) {
                log.debug("Fallback credit lookup failed for sellerId={}", sellerId, e);
            }
        }
        return result;
    }
}
