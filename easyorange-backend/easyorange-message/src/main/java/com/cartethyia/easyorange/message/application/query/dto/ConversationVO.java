package com.cartethyia.easyorange.message.application.query.dto;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 会话内单条消息响应 —— 与 WebSocket {@code chatFrame} 同字段集，前端把两种来源当同一对象处理，
 * 少一个字段就得在客户端补默认值。
 *
 * <p>{@code msgStatus} 与 {@code recalledAt} 必须下发：撤回只把状态改成 {@code RECALLED} 并盖时间戳，
 * 不清 {@code content}（原文留在库里便于审计）。前端没有这两个字段时，撤回过的消息刷新页面会
 * 重新显示原文 —— 撤回形同虚设。
 *
 * <p>已读语义走 {@code isRead}（{@code eo_message.is_read}），不走 {@code msgStatus}：
 * 后者只承载发送与撤回两个事实。
 */
@Builder
public record ConversationVO(
        String id,
        String conversationId,
        String senderId,
        String senderName,
        String senderAvatar,
        String receiverId,
        String receiverName,
        String receiverAvatar,
        Integer type,
        String title,
        String content,
        Integer isRead,
        LocalDateTime readTime,
        String msgStatus,
        LocalDateTime recalledAt,
        LocalDateTime createTime) {}
