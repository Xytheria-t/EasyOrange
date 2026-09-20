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
    TOKEN_BUDGET_EXCEEDED("B8001", "今日 AI 调用预算已用尽，请明天再试"),

    /**
     * 拍照识别没有产出可用结果（模型返回空 / 输出不可解析 / 供应商异常）。
     * <p>
     * 走显式错误码而不是 HTTP 200 + {@code data:null}：发布助手没有「部分可用」的结果可言，
     * 静默成功会让用户点了按钮、等一会儿、零反馈 —— 失败必须是用户看得见的事实。
     */
    AI_UNAVAILABLE("B8002", "AI 服务暂时不可用，请稍后重试");

    private final String code;
    private final String message;
}
