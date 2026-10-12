package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.application.support.TokenBudgetGuard;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 预算切面 — 拦截标注 {@link TokenBudget} 的方法，调用前原子预留 {@code maxTokensPerCall}：预留在存储侧
 * 单次完成（读-判-记无窗口），失败抛 {@link TokenBudgetExceededException} 且目标方法不执行；请求结束（含异常）
 * 释放预留，真实用量由 {@code ModelCallRecorder} 按供应商回报另记 —— 服务方法只返回业务 DTO，切面拿不到真实
 * token 数，按上限记账会与真实消耗差一个量级。
 * <p>
 * <b>配置优先</b>：{@code easyorange.ai.budget.scenarios.<scenario>} 覆盖注解默认值（解析在 {@link TokenBudgetGuard}）
 * —— 注解给编译期可见的兜底契约，运维改限额不发版。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easyorange.ai.budget.enabled", matchIfMissing = true)
public class TokenBudgetAspect {

    private final TokenBudgetGuard tokenBudgetGuard;

    @Around("@annotation(tokenBudget)")
    public Object aroundBudget(ProceedingJoinPoint pjp, TokenBudget tokenBudget) throws Throwable {
        var scenario = tokenBudget.scenario();
        var budget = tokenBudgetGuard.resolve(scenario, tokenBudget.maxTokensPerCall(), tokenBudget.dailyTokenLimit());

        var reservation = tokenBudgetGuard.tryReserve(scenario, budget);
        if (reservation == null) {
            log.warn(
                    "action=token_budget_exceeded, scenario={}, maxPerCall={}, limit={}",
                    scenario,
                    budget.maxTokensPerCall(),
                    budget.dailyTokenLimit());
            throw new TokenBudgetExceededException();
        }
        try {
            return pjp.proceed();
        } finally {
            reservation.release();
        }
    }
}
