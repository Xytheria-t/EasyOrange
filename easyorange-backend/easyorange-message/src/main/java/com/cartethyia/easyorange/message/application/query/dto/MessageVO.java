package com.cartethyia.easyorange.message.application.query.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息列表响应项 —— 与 {@code ConversationVO} 同形状（收发方 id / 昵称 / 头像齐全），
 * 前端两个页面可共用一套渲染。
 * <p>
 * 不含 updateTime：聚合根不承载该字段（消息只有创建与已读 / 撤回这几个时间点），
 * 响应里挂一个恒为 null 的字段只会让人以为有更新时间。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private String id;

    private String senderId;

    private String senderName;

    private String senderAvatar;

    private String receiverId;

    private String receiverName;

    private String receiverAvatar;

    private Integer type;

    private String typeDesc;

    private String title;

    private String content;

    private Integer isRead;

    private String readDesc;

    private String businessId;

    private LocalDateTime createTime;
}
