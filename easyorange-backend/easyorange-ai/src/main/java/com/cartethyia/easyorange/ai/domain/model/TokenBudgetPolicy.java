package com.cartethyia.easyorange.ai.domain.model;

/**
 * 日预算判定公式 —— 全链路唯一实现。
 * <p>
 * 同一条 chat 链路有两条执行路径会问「这次调用还发不发得出去」：非流式 {@code answer()} 走
 * {@code @TokenBudget} 切面，流式 {@code streamAnswer()} 与工具循环中途降级走 {@code ChatBudgetGuard}。
 * 判据写在两处时，两边各改一次就会静默分叉（一处放行、一处拒绝），而症状只在超限当天出现。
 * 所以判定只此一份，两侧只负责「取哪个场景的配置与用量」。
 */
public final class TokenBudgetPolicy {

    private TokenBudgetPolicy() {}

    /**
     * 累计用量 + 本次预估越过日限即已耗尽；{@code dailyTokenLimit <= 0} 表示不限（0 是「关」而不是「余量 0」）。
     *
     * @param usedTokens      今日该场景累计用量（真实记账值）
     * @param maxTokensPerCall 本次调用按场景上限估的预留量
     * @param dailyTokenLimit  日限额
     */
    public static boolean exhausted(int usedTokens, int maxTokensPerCall, int dailyTokenLimit) {
        return dailyTokenLimit > 0 && usedTokens + maxTokensPerCall > dailyTokenLimit;
    }
}
