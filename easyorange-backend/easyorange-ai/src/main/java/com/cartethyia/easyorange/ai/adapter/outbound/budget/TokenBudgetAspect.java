package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.model.TokenBudgetPolicy;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 预算切面 — 拦截标注 {@link TokenBudget} 的方法，调用前检查日预算：累计用量 + 本次预估越过日限就抛
 * {@link TokenBudgetExceededException}，目标方法不执行（判定式见 {@link TokenBudgetPolicy}，与流式预检 /
 * 循环降级共用同一份）。
 * <p>
 * <b>配置优先</b>：{@code easyorange.ai.budget.scenarios.<scenario>} 覆盖注解默认值 —— 注解给编译期可见的兜底
 * 契约，运维改限额不发版。
 * <p>
 * <b>只做前置检查，不做记账</b>：真实用量在 {@code AiCallRecorder} 拿到供应商回报 tokens 处记 —— 服务方法只返回
 * 业务 DTO，拿 {@code maxTokensPerCall} 当用量累加会与真实消耗差一个量级。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easyorange.ai.budget.enabled", matchIfMissing = true)
public class TokenBudgetAspect {

    private final TokenBudgetStorePort budgetStore;
    private final AiProperties aiProperties;

    @Around("@annotation(tokenBudget)")
    public Object aroundBudget(ProceedingJoinPoint pjp, TokenBudget tokenBudget) throws Throwable {
        var scenario = tokenBudget.scenario();
        var resolved = resolveBudget(scenario, tokenBudget);
        var maxPerCall = resolved.maxTokensPerCall();
        var dailyLimit = resolved.dailyTokenLimit();

        var used = budgetStore
                .getTodayUsage(scenario)
                .map(TokenBudgetStorePort.TokenUsage::total)
                .orElse(0);

        if (TokenBudgetPolicy.exhausted(used, maxPerCall, dailyLimit)) {
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
