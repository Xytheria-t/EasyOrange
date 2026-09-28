package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息列表查询入参 —— 只按「我的收件 + 类型 / 已读态」过滤。
 * <p>
 * 不提供 senderId / receiverId 过滤：列表恒以当前登录用户为接收方（见
 * {@code MessageQueryRepository#findByReceiverId}），再暴露收发方字段只会让人以为能查别人的消息；
 * 且 ID 全项目统一 UUID v7 String，Long 形状本就是错的。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryMessageRequest extends PageRequest {

    private Integer type;

    private Integer isRead;
}
