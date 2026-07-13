package com.cartethyia.easyorange.ai.budget;

/**
 * Token 预算超限异常 — 当 AI 调用场景的 token 用量超过配置的预算时抛出。
 * <p>
 * 调用方可捕获此异常决定降级策略（如返回缓存结果、返回默认值等）。
 */
public class TokenBudgetExceededException extends RuntimeException {

    public TokenBudgetExceededException(String scenario, int used, int limit) {
        super("Token budget exceeded for scenario '%s': used=%d, limit=%d".formatted(scenario, used, limit));
    }
}
