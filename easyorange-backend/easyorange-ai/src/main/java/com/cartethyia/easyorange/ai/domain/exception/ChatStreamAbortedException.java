package com.cartethyia.easyorange.ai.domain.exception;

/**
 * 客户端已离开流式回答（刷新 / 关页 / emitter 已完成）— 由 {@link ChatStreamHandler} 的回调实现抛出，
 * 表示事件已无听众，流应立即作废。服务侧必须与「模型故障」分开收尾：不打 ERROR、不计入
 * {@code easyorange.ai.chat.degraded}、不回 {@code onError}（客户端中途离开曾被兜底 catch 记成
 * {@code reason=unavailable}，每次刷新都污染降级率与错误日志）。
 * <p>
 * {@code partialText} 是中断前已推给客户端的半截回答（无输出为空串）：中断不回滚这轮——预算已花、
 * 用户已看到，上层据此把该轮落进会话历史，刷新后追问才有上下文。
 * <p>
 * <b>不继承 {@code BaseBusinessException} 是刻意的</b>：控制流信号不是领域错误、从不映射 HTTP 响应。
 */
public class ChatStreamAbortedException extends RuntimeException {

    private final String partialText;

    public ChatStreamAbortedException(Throwable cause) {
        this("", cause);
    }

    public ChatStreamAbortedException(String partialText, Throwable cause) {
        super(cause);
        this.partialText = partialText;
    }

    public String partialText() {
        return partialText;
    }
}
