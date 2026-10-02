package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** {@code isRead} 声明为枚举而非 boolean，故Lombok 生成的是 {@code getIsRead()}，与仓储侧的 Lambda 引用一致。 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message")
public class MessageDO extends BaseDO {

    private String senderId;
    private String receiverId;
    private Integer type;
    private String title;
    private String content;
    private ReadStatus isRead;
    private LocalDateTime readTime;
    private String businessId;
    private String conversationId;
    private MessageStatus msgStatus;
    private LocalDateTime recalledAt;
}
