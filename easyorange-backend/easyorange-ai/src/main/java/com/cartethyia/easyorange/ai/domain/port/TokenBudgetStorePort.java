package com.cartethyia.easyorange.ai.domain.port;

import java.util.Optional;

/**
 * Token 预算存储接口 — 记录和查询按场景隔离的每日 token 用量。
 */
public interface TokenBudgetStorePort {

    record TokenUsage(int inputTokens, int outputTokens, long timestamp) {
        public int total() {
            return inputTokens + outputTokens;
        }
    }

    /** 指定场景今日的累计用量，无记录返回 {@link Optional#empty()}。 */
    Optional<TokenUsage> getTodayUsage(String scenario);

    /** 记录指定场景的 token 用量（累加到今日统计）。 */
    void recordUsage(String scenario, int inputTokens, int outputTokens);
}
