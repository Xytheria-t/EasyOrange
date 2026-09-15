package com.cartethyia.easyorange.ai.domain.constant;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ai 模块错误码
 * <p>
 * 错误码范围：B8001-B8999（B8xxx-B9xxx 为预留段，见 doc/agents/架构参考.md「错误码模块分配表」）。
 * HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 * <p>
 * 预算超限走 B 段（400）而非限流的 A0429：前端 {@code isRetryable} 把 429 视为可重试
 * （`request.ts`），日预算按天耗尽、重试无意义，复用 429 会把配额耗尽变成重试风暴。
 * </p>
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum AiResultCode implements IResultCode {
    TOKEN_BUDGET_EXCEEDED("B8001", "今日 AI 调用预算已用尽，请明天再试");

    private final String code;
    private final String message;
}
