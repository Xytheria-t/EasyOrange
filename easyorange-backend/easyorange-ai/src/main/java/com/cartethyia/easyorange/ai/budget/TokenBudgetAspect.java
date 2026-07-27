package com.cartethyia.easyorange.ai.budget;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
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
 * <b>配置优先级</b>：{@code easyorange.ai.budget.scenarios.<scenario>} 覆盖注解默认值，
 * 缺失时回退到注解声明的 {@code maxTokensPerCall} / {@code dailyTokenLimit}。
 * 这样注解提供编译期可见的兜底契约，运维可通过配置热更新限额而无需发版。
 * <p>
 * <b>调用前</b>：若 {@code dailyTokenLimit > 0} 且 累计用量 + 本次预估 &gt; dailyTokenLimit，
 * 上报 {@link AiMetricsService#recordTokenBudgetExceeded} 后抛 {@link TokenBudgetExceededException}，
 * 目标方法不执行。
 * <p>
 * <b>调用后</b>：将 {@code maxTokensPerCall} 作为预估用量记入存储，
 * 上报 {@link AiMetricsService#recordTokenBudgetUsage}（含本次用量 + 当日累计）。
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
    private final AiProperties aiProperties;
    private final AiMetricsService aiMetricsService;

    @Around("@annotation(tokenBudget)")
    public Object aroundBudget(ProceedingJoinPoint pjp, TokenBudget tokenBudget) throws Throwable {
        var scenario = tokenBudget.scenario();
        var resolved = resolveBudget(scenario, tokenBudget);
        var maxPerCall = resolved.maxTokensPerCall();
        var dailyLimit = resolved.dailyTokenLimit();

        var used = budgetStore.getTodayUsage(scenario)
                .map(TokenBudgetStore.TokenUsage::total)
                .orElse(0);

        // 预算检查（dailyTokenLimit=0 表示不限）
        if (dailyLimit > 0 && used + maxPerCall > dailyLimit) {
            log.warn("action=token_budget_exceeded, scenario={}, used={}, maxPerCall={}, limit={}",
                    scenario, used, maxPerCall, dailyLimit);
            aiMetricsService.recordTokenBudgetExceeded(scenario);
            throw new TokenBudgetExceededException(scenario, used, dailyLimit);
        }

        // 执行目标方法
        var result = pjp.proceed();

        // 记录预估用量（LlmPort 返回 String，无法获取真实 token 数，用 maxTokensPerCall 估算）
        budgetStore.recordUsage(scenario, maxPerCall, 0);
        var dailyTotal = used + maxPerCall;
        aiMetricsService.recordTokenBudgetUsage(scenario, maxPerCall, dailyTotal);

        return result;
    }

    /**
     * 解析场景预算：配置优先，注解兜底。
     * <p>
     * 配置中存在 scenario 条目时用配置值，否则用注解声明的默认值。
     * 这让注解成为编译期契约，配置成为运行期调优旋钮。
     */
    private ResolvedBudget resolveBudget(String scenario, TokenBudget annotation) {
        var scenarioConfig = aiProperties.getBudget().resolve(scenario);
        if (scenarioConfig != null) {
            return new ResolvedBudget(scenarioConfig.getMaxTokensPerCall(), scenarioConfig.getDailyTokenLimit());
        }
        return new ResolvedBudget(annotation.maxTokensPerCall(), annotation.dailyTokenLimit());
    }

    private record ResolvedBudget(int maxTokensPerCall, int dailyTokenLimit) {}
}
