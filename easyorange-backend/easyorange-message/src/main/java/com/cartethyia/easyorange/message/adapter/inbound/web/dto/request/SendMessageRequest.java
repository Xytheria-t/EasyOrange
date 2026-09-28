package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求（HTTP 请求体）—— 只承载传输层语义，不含会话 ID。
 * <p>
 * 会话 ID 不接受前端入参：它是「我 + 对方」的确定性函数，由 {@code Message} 聚合根算出后落库，
 * 否则同一个房间会被客户端命名成两个值，{@code idx_eo_message_conversation_time} 也失去意义。
 * <p>
 * type 可空（缺省归一化为聊天消息，见 {@code MessageCommandHandler#normalizeType}）。
 */
public record SendMessageRequest(
        @NotBlank(message = "接收方不能为空") String receiverId,

        Integer type,

        @Size(max = 100, message = "标题最长 100 字") String title,

        @NotBlank(message = "消息内容不能为空") @Size(max = 2000, message = "消息内容最长 2000 字")
        String content,

        String businessId) {}
