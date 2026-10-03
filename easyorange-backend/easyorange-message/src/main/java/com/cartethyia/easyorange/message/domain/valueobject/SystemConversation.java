package com.cartethyia.easyorange.message.domain.valueobject;

/**
 * 系统会话的占位 ID —— 会话列表用它把「发送方为 null」的消息归并成一条（{@code sender_id IS NULL}
 * 没法作为会话对方的 ID，强行用 null 会得到 null key）。
 * <p>
 * 查详情时必须把这个占位翻译回 {@code sender_id IS NULL}：直接拿它去比 {@code sender_id}，
 * SQL 里 {@code NULL = 'system'} 恒不成立，系统会话会永远查不到消息、点开是一片空白。
 */
public final class SystemConversation {

    public static final String ID = "system";

    private SystemConversation() {}
}
