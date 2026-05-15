package com.cartethyia.easyorange.message.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息 VO
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;

    private Long senderId;

    private String senderName;

    private String senderAvatar;

    private Long receiverId;

    private String receiverName;

    private Integer type;

    private String typeDesc;

    private String title;

    private String content;

    private Integer isRead;

    private String readDesc;

    private Long businessId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
