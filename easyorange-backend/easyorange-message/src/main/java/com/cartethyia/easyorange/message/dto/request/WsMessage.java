package com.cartethyia.easyorange.message.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private Long id;

    private Long senderId;

    private Long receiverId;

    private String senderName;

    private Integer type;

    private String title;

    private String content;

    private Long businessId;

    private LocalDateTime createTime;
}
