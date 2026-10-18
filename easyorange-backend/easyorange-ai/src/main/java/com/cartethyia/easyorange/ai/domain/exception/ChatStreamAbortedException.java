package com.cartethyia.easyorange.ai.domain.exception;

/**
 * 客户端已离开流式回答（刷新 / 关页 / emitter 已完成）— 事件已无听众，流立即作废，由
 * {@link com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler} 回调实现抛出。收尾必须与「模型故障」分开：
 * 不打 ERROR、不计入 {@code easyorange.ai.chat.degraded}、不回 {@code onError} —— 混淆会记成
 * {@code reason=unavailable}，每次刷新都污染降级率与错误日志。
 * <p>
 * {@code partialText} 是中断前已推给客户端的半截回答（无输出为空串）；中断不回滚这轮（预算已花、
 * 用户已看到），上层据此把该轮落进会话历史。不继承 {@code BaseBusinessException} 是刻意的：控制流
 * 信号不映射 HTTP 响应。
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
