package com.cartethyia.easyorange.message.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageSubscriptionVO {

    private Long id;

    private String messageType;

    private String pushChannel;

    private Boolean enabled;
}
