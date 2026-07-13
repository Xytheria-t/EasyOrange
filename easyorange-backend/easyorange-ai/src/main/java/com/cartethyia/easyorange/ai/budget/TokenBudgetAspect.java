package com.cartethyia.easyorange.ai.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 预算切面 — 拦截标注 {@link TokenBudget} 的方法，调用前检查日预算是否超限。
 * <p>
 * 调用前：若 {@code dailyTokenLimit > 0} 且 累计用量 + 本次预估 &gt; dailyTokenLimit，
 * 抛 {@link TokenBudgetExceededException}，目标方法不执行。
 * <p>
 * 调用后：将 {@code maxTokensPerCall} 作为预估用量记入存储。
 * 由于 {@code LlmPort} 返回 {@code String}，无法获取真实 token 数，
 * 这里用注解声明的 {@code maxTokensPerCall} 作为估算值（YAGNI：不引入 token 计数器）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easyorange.ai.budget.enabled", matchIfMissing = true)
public class TokenBudgetAspect {

    private final TokenBudgetStore budgetStore;

    @Around("@annotation(tokenBudget)")
    public Object aroundBudget(ProceedingJoinPoint pjp, TokenBudget tokenBudget) throws Throwable {
        var scenario = tokenBudget.scenario();
        var maxPerCall = tokenBudget.maxTokensPerCall();
        var dailyLimit = tokenBudget.dailyTokenLimit();

        // 预算检查（dailyTokenLimit=0 表示不限）
        if (dailyLimit > 0) {
            var used = budgetStore.getTodayUsage(scenario)
                    .map(TokenBudgetStore.TokenUsage::total)
                    .orElse(0);
            if (used + maxPerCall > dailyLimit) {
                log.warn("action=token_budget_exceeded, scenario={}, used={}, maxPerCall={}, limit={}",
                        scenario, used, maxPerCall, dailyLimit);
                throw new TokenBudgetExceededException(scenario, used, dailyLimit);
            }
        }

        // 执行目标方法
        var result = pjp.proceed();

        // 记录预估用量（LlmPort 返回 String，无法获取真实 token 数，用 maxTokensPerCall 估算）
        budgetStore.recordUsage(scenario, maxPerCall, 0);

        return result;
    }
}
