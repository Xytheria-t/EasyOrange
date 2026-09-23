package com.cartethyia.easyorange.ai.domain.port;

/**
 * 客户端已离开流式回答（刷新 / 关页 / emitter 已完成）— 由 {@link ChatStreamHandler} 的回调实现抛出，
 * 表示事件已无听众，流应立即作废。
 * <p>
 * 服务侧（{@code AiChatService#streamAnswer}）必须把它与「模型故障」分开收尾：
 * 不打 ERROR、不计入 {@code easyorange.ai.chat.degraded}、不回 {@code onError}
 *（客户端中途离开曾被兜底 catch 记成 {@code reason=unavailable} 的 ERROR，
 * 每次刷新都污染降级率与错误日志）。
 */
public class ChatStreamAbortedException extends RuntimeException {

    public ChatStreamAbortedException(Throwable cause) {
        super(cause);
    }
}
