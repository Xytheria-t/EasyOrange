package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.TokenBudgetPolicy;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 对话场景的日预算前置判定 — 命中「流式入口预检」与「循环中途降级」两个生效点。
 * <p>
 * 判定式单点在 {@link TokenBudgetPolicy}：AOP 切面（非流式 {@code answer}）与本类（流式 + 循环降级）都调它。
 * 流式路径手动调而不挂 {@code @TokenBudget}（切面同样会拦）：切面在<b>代理边界</b>抛预算异常，早于方法体，
 * {@code streamAnswer} 内部把它转 error 事件的路由接不到，预算提示会落成 Controller 的通用降级文案。
 * <p>
 * 也不放进工具调用循环：预算是「这次调用还能不能发出去」的治理决策，属 chat 特性而非循环执行器。
 * <p>
 * <b>配置优先、注解兜底</b>（与 {@code TokenBudgetAspect} 同一口径）；兜底值必须与
 * {@code @TokenBudget(scenario = "chat")} 注解一致，{@code ChatBudgetGuardTest} 反射断言两者相等。
 */
@Component
@RequiredArgsConstructor
public class ChatBudgetGuard {

    /** 预算场景键取自 {@link AiCallScope}（枚举名小写），与 {@code @TokenBudget(scenario=...)} 单点同源不重写。 */
    private static final String CHAT_SCENARIO = AiCallScope.CHAT.budgetScenario();

    /** 与 {@code @TokenBudget(scenario="chat")} 注解默认值一致（yaml 场景缺失时兜底；契约由测试锁死）。 */
    static final int DEFAULT_MAX_TOKENS_PER_CALL = 1500;

    static final int DEFAULT_DAILY_LIMIT = 300_000;

    private final TokenBudgetStorePort budgetStore;
    private final AiProperties aiProperties;

    /** 日预算余量不足即 true；{@code dailyTokenLimit <= 0} 表示不限。 */
    public boolean exhausted() {
        int used = budgetStore
                .getTodayUsage(CHAT_SCENARIO)
                .map(TokenBudgetStorePort.TokenUsage::total)
                .orElse(0);
        var cfg = aiProperties.budget().resolve(CHAT_SCENARIO);
        int maxPerCall = cfg != null ? cfg.maxTokensPerCall() : DEFAULT_MAX_TOKENS_PER_CALL;
        int dailyLimit = cfg != null ? cfg.dailyTokenLimit() : DEFAULT_DAILY_LIMIT;
        return TokenBudgetPolicy.exhausted(used, maxPerCall, dailyLimit);
    }
}
