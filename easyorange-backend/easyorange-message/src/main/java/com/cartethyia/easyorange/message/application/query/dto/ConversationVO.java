package com.cartethyia.easyorange.message.application.query.dto;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 会话内单条消息响应 —— 与 WebSocket {@code chatFrame} 同字段集，前端把两种来源当同一对象处理，
 * 少一个字段就得在客户端补默认值。
 *
 * <p>字段名 {@code status} 必须与 {@code chatFrame} 一致：前端 {@code normalizeChatMessage} 只认这一个键，
 * 这条路径下发成 {@code msgStatus} 时撤回状态恒为 undefined，刷新后已撤回消息重新显示原文。
 * 列名仍是 {@code eo_message.msg_status}，改名只发生在传输层。
 *
 * <p>{@code status} 与 {@code recalledAt} 必须下发：撤回只改状态盖时间戳，不清 {@code content}（原文留库便于审计）。
 *
 * <p>已读语义走 {@code isRead}，不走 {@code status}：后者只承载发送与撤回两个事实。
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
        String status,
        LocalDateTime recalledAt,
        LocalDateTime createTime) {}
