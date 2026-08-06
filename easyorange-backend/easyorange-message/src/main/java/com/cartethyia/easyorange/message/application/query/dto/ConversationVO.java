package com.cartethyia.easyorange.message.application.query.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private String id;

    private String senderId;

    private String senderName;

    private String senderAvatar;

    private String receiverId;

    private String receiverName;

    private String receiverAvatar;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;
}
