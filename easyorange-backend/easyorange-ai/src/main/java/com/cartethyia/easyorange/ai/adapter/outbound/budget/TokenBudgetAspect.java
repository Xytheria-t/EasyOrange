package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 预算切面 — 拦截标注 {@link TokenBudget} 的方法，调用前原子预留 {@code maxTokensPerCall}：
 * check-and-reserve 在存储侧单次完成（读-判-记之间无窗口），预留失败抛 {@link TokenBudgetExceededException}
 * 目标方法不执行；请求结束（含异常）释放预留，真实用量由 {@code AiCallRecorder} 按供应商回报另记 —— 服务方法
 * 只返回业务 DTO，切面拿不到真实 token 数，按上限记账会与真实消耗差一个量级。
 * <p>
 * <b>配置优先</b>：{@code easyorange.ai.budget.scenarios.<scenario>} 覆盖注解默认值 —— 注解给编译期可见的兜底
 * 契约，运维改限额不发版。
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

        var reservation = budgetStore.tryReserve(scenario, resolved.maxTokensPerCall(), resolved.dailyTokenLimit());
        if (reservation == null) {
            log.warn(
                    "action=token_budget_exceeded, scenario={}, maxPerCall={}, limit={}",
                    scenario,
                    resolved.maxTokensPerCall(),
                    resolved.dailyTokenLimit());
            throw new TokenBudgetExceededException();
        }
        try {
            return pjp.proceed();
        } finally {
            reservation.release();
        }
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
