package com.cartethyia.easyorange.ai.budget;

import com.cartethyia.easyorange.ai.enums.AiResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * Token 预算超限异常 — 当 AI 调用场景的 token 用量超过配置的预算时抛出。
 * <p>
 * 码取 {@link AiResultCode#TOKEN_BUDGET_EXCEEDED}（B8001 → 400），响应体只带面向用户的中文提示；
 * 场景 / 已用量 / 限额由 {@link TokenBudgetAspect} 在抛出前记入 {@code action=token_budget_exceeded} 日志。
 * <p>
 * 调用方可捕获此异常决定降级策略（如返回缓存结果、返回默认值等）：
 * 流式对话在 {@code AiChatService} 捕获后转 SSE error 事件。
 */
public class TokenBudgetExceededException extends BaseBusinessException {

    public TokenBudgetExceededException() {
        super(AiResultCode.TOKEN_BUDGET_EXCEEDED);
    }
}
