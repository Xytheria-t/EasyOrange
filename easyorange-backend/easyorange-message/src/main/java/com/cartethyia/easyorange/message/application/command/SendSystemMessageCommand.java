package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.message.domain.enums.MessageBizType;

/**
 * 发系统通知的命令 —— {@code bizType} 声明 {@code businessId} 指向哪类业务对象，
 * 读侧据此决定点击跳转目标；缺省按 {@link MessageBizType#NONE} 收敛（不给跳转入口）。
 */
public record SendSystemMessageCommand(
        String receiverId, String title, String content, String businessId, MessageBizType bizType) {

    public SendSystemMessageCommand {
        bizType = bizType == null ? MessageBizType.NONE : bizType;
    }
}
