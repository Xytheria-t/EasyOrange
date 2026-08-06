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
public class ConversationListVO {

    private String targetUserId;

    private String targetUserName;

    private String targetUserAvatar;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;
}
