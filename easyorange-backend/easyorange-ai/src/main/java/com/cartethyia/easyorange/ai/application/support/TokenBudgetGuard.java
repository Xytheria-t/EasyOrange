package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.TokenBudgetPolicy;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort.TokenReservation;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * scenario 预算的解析与执行单点 —「yaml 配置优先、调用方兜底值其次」只在这里实现一次：切面前置检查、
 * chat 流式预留、chat / listing 循环中途判定全部经本类，杜绝多份同规则实现静默分叉（一处放行一处拒绝，
 * 症状只在超限当天出现）。兜底值由调用方传入：切面给注解、两条链路给各自与 {@code @TokenBudget} 反射
 * 契约锁死的常量；判定式收口在 {@link TokenBudgetPolicy}。
 */
@Component
@RequiredArgsConstructor
public class TokenBudgetGuard {

    private final TokenBudgetStorePort budgetStore;
    private final AiProperties aiProperties;

    /** 解析场景预算 — 场景未配置时回退调用方兜底值；单独暴露是给切面按生效值写超限日志（与预留同一次解析）。 */
    public ResolvedBudget resolve(String scenario, int fallbackMaxPerCall, int fallbackDailyLimit) {
        var cfg = aiProperties.budget().resolve(scenario);
        return cfg != null
                ? new ResolvedBudget(cfg.maxTokensPerCall(), cfg.dailyTokenLimit())
                : new ResolvedBudget(fallbackMaxPerCall, fallbackDailyLimit);
    }

    /** 原子预留 — null 即场景日预算耗尽，转哪条降级由调用方定（切面抛异常 / 流式转 error 事件）。 */
    public @Nullable TokenReservation tryReserve(String scenario, ResolvedBudget budget) {
        return budgetStore.tryReserve(scenario, budget.maxTokensPerCall(), budget.dailyTokenLimit());
    }

    /** 只读检查 — 用量含在途预留；{@code dailyTokenLimit <= 0} 表示不限，判据与切面预留同源。 */
    public boolean exhausted(String scenario, int fallbackMaxPerCall, int fallbackDailyLimit) {
        int used = budgetStore
                .getTodayUsage(scenario)
                .map(TokenBudgetStorePort.TokenUsage::total)
                .orElse(0);
        var budget = resolve(scenario, fallbackMaxPerCall, fallbackDailyLimit);
        return TokenBudgetPolicy.exhausted(used, budget.maxTokensPerCall(), budget.dailyTokenLimit());
    }

    /** 生效预算 — 配置解析结果或调用方兜底值。 */
    public record ResolvedBudget(int maxTokensPerCall, int dailyTokenLimit) {}
}
