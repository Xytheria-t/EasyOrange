package com.cartethyia.easyorange.ai.domain.exception;

import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * Token 预算超限异常 — 码取 {@link AiResultCode#TOKEN_BUDGET_EXCEEDED}（B8001 → 400），响应体只带
 * 面向用户的中文提示；场景 / 已用量 / 限额由 {@link TokenBudgetAspect} 在抛出前记入
 * {@code action=token_budget_exceeded} 日志。
 * <p>
 * 调用方可捕获决定降级：流式对话在 {@code AiChatAppService} 转 SSE error 事件。
 */
public class TokenBudgetExceededException extends BaseBusinessException {

    public TokenBudgetExceededException() {
        super(AiResultCode.TOKEN_BUDGET_EXCEEDED);
    }
}
