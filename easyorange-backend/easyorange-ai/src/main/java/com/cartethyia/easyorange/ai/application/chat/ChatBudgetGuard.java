package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.TokenBudgetGuard;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort.TokenReservation;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 对话场景的日预算前置判定 —「流式入口预留」与「循环中途降级」两个生效点；解析与判定在
 * {@link TokenBudgetGuard}（配置优先、此处兜底常量），本类只锁 chat 的场景键与兜底值。
 * <p>
 * 流式路径手动预留而不挂 {@code @TokenBudget}：切面在<b>代理边界</b>抛预算异常、早于方法体，{@code streamAnswer}
 * 转 error 事件的路由接不到，预算提示会落成 Controller 的通用降级文案；预留走 {@link #tryAcquire}（与切面同一
 * 原子预留协议，请求结束释放），{@link #exhausted} 是循环中途的只读检查（用量含在途预留）。不放进工具调用循环：
 * 预算是「这次调用还能不能发出去」的治理决策，属 chat 特性而非循环执行器；兜底值须与
 * {@code @TokenBudget(scenario = "chat")} 一致，{@code ChatBudgetGuardTest} 反射断言两者相等。
 */
@Component
@RequiredArgsConstructor
public class ChatBudgetGuard {

    /** 预算场景键取自 {@link AiCallScope}（枚举名小写），与 {@code @TokenBudget(scenario=...)} 单点同源不重写。 */
    private static final String CHAT_SCENARIO = AiCallScope.CHAT.budgetScenario();

    /** yaml 场景缺失时的兜底 —— 须与 {@code @TokenBudget(scenario="chat")} 注解默认值一致（契约由测试反射锁死）。 */
    static final int DEFAULT_MAX_TOKENS_PER_CALL = 1500;

    static final int DEFAULT_DAILY_LIMIT = 300_000;

    private final TokenBudgetGuard tokenBudgetGuard;

    /** 流式入口的原子预留 — 预留失败（返回 null）即日预算耗尽，调用方转 error 事件；成功必须配对释放（请求结束 finally），
     * 与切面同一条 tryReserve 协议，只是流式路径没有切面边界。 */
    public @Nullable TokenReservation tryAcquire() {
        var budget = tokenBudgetGuard.resolve(CHAT_SCENARIO, DEFAULT_MAX_TOKENS_PER_CALL, DEFAULT_DAILY_LIMIT);
        return tokenBudgetGuard.tryReserve(CHAT_SCENARIO, budget);
    }

    /** 日预算余量不足即 true；{@code dailyTokenLimit <= 0} 表示不限。用量含在途预留（并发口径）。 */
    public boolean exhausted() {
        return tokenBudgetGuard.exhausted(CHAT_SCENARIO, DEFAULT_MAX_TOKENS_PER_CALL, DEFAULT_DAILY_LIMIT);
    }
}
