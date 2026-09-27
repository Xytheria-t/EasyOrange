package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 预算切面 — 拦截标注 {@link TokenBudget} 的方法，调用前检查日预算：
 * {@code dailyTokenLimit > 0} 且 累计用量 + 本次预估 &gt; dailyTokenLimit 时抛
 * {@link TokenBudgetExceededException}，目标方法不执行。
 * <p>
 * <b>配置优先</b>：{@code easyorange.ai.budget.scenarios.<scenario>} 覆盖注解默认值 —— 注解提供
 * 编译期可见的兜底契约，运维通过配置热更新限额而无需发版。
 * <p>
 * <b>只做前置检查，不做记账</b>：真实用量在 {@code AiModelSupport} 拿到供应商回报 tokens 的地方记。
 * 记账不能放这里：服务方法返回业务 DTO，只能把 {@code maxTokensPerCall} 当用量累加，
 * 数字与真实消耗差一个量级。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easyorange.ai.budget.enabled", matchIfMissing = true)
public class TokenBudgetAspect {

    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;

    @Around("@annotation(tokenBudget)")
    public Object aroundBudget(ProceedingJoinPoint pjp, TokenBudget tokenBudget) throws Throwable {
        var scenario = tokenBudget.scenario();
        var resolved = resolveBudget(scenario, tokenBudget);
        var maxPerCall = resolved.maxTokensPerCall();
        var dailyLimit = resolved.dailyTokenLimit();

        var used = budgetStore
                .getTodayUsage(scenario)
                .map(TokenBudgetStore.TokenUsage::total)
                .orElse(0);

        // 预算检查（dailyTokenLimit=0 表示不限）
        if (dailyLimit > 0 && used + maxPerCall > dailyLimit) {
            log.warn(
                    "action=token_budget_exceeded, scenario={}, used={}, maxPerCall={}, limit={}",
                    scenario,
                    used,
                    maxPerCall,
                    dailyLimit);
            throw new TokenBudgetExceededException();
        }

        return pjp.proceed();
    }

    /** 解析场景预算：配置优先，注解兜底 —— 注解是编译期契约，配置是运行期调优旋钮。 */
    private ResolvedBudget resolveBudget(String scenario, TokenBudget annotation) {
        var scenarioConfig = aiProperties.budget().resolve(scenario);
        if (scenarioConfig != null) {
            return new ResolvedBudget(scenarioConfig.maxTokensPerCall(), scenarioConfig.dailyTokenLimit());
        }
        return new ResolvedBudget(annotation.maxTokensPerCall(), annotation.dailyTokenLimit());
    }

    private record ResolvedBudget(int maxTokensPerCall, int dailyTokenLimit) {}
}
