package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息传输对象
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsMessage {

    private String id;

    private String senderId;

    private String receiverId;

    private String senderName;

    private Integer type;

    private String title;

    private String content;

    private String businessId;

    private LocalDateTime createTime;

    private String conversationId;

    private String targetUserId;
}
