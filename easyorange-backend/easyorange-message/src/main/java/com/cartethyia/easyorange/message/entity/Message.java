package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message")
public class Message extends BaseDO {

    private Long senderId;

    private Long receiverId;

    private Integer type;

    private String title;

    private String content;

    private Integer isRead;

    private Long businessId;

    private Long conversationId;
}
