package com.cartethyia.easyorange.ai.domain.model;

/**
 * 日预算判定公式 —— 只读检查的唯一实现（工具循环中途降级走这里）。
 * <p>
 * 入口侧（切面 / 流式预留）的判定内嵌在存储的原子预留里（Redis Lua / 内存 CAS，读-判-记一个窗口闭合），
 * 与本式是同一条公式 —— 判据写在两处时，一边改一次就会静默分叉（一处放行、一处拒绝），症状只在超限当天出现，
 * 改动必须同步。
 */
public final class TokenBudgetPolicy {

    private TokenBudgetPolicy() {}

    /**
     * 累计占用 + 本次预估越过日限即已耗尽；{@code dailyTokenLimit <= 0} 表示不限（0 是「关」而不是「余量 0」）。
     *
     * @param usedTokens      今日该场景累计占用（真实记账 + 在途预留的并发口径）
     * @param maxTokensPerCall 本次调用按场景上限估的预留量
     * @param dailyTokenLimit  日限额
     */
    public static boolean exhausted(int usedTokens, int maxTokensPerCall, int dailyTokenLimit) {
        return dailyTokenLimit > 0 && usedTokens + maxTokensPerCall > dailyTokenLimit;
    }
}
