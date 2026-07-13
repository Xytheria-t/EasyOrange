package com.cartethyia.easyorange.ai.budget;

import java.util.Optional;

/**
 * Token 预算存储接口 — 记录和查询按场景隔离的每日 token 用量。
 */
public interface TokenBudgetStore {

    /**
     * Token 用量记录。
     *
     * @param inputTokens  输入 token 数
     * @param outputTokens 输出 token 数
     * @param timestamp    记录时间戳（毫秒）
     */
    record TokenUsage(int inputTokens, int outputTokens, long timestamp) {
        public int total() {
            return inputTokens + outputTokens;
        }
    }

    /**
     * 获取指定场景今日的累计用量。
     *
     * @param scenario 场景名
     * @return 今日用量，无记录时返回 {@link Optional#empty()}
     */
    Optional<TokenUsage> getTodayUsage(String scenario);

    /**
     * 记录指定场景的 token 用量（累加到今日统计）。
     *
     * @param scenario      场景名
     * @param inputTokens   输入 token 数
     * @param outputTokens  输出 token 数
     */
    void recordUsage(String scenario, int inputTokens, int outputTokens);
}
