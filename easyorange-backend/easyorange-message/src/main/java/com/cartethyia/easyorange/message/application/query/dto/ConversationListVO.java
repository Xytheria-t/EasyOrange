package com.cartethyia.easyorange.message.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListVO {

    private Long targetUserId;

    private String targetUserName;

    private String targetUserAvatar;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;
}
