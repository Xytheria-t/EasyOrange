package com.cartethyia.easyorange.message.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageSubscriptionVO {

    private String id;

    private String messageType;

    private String pushChannel;

    private Boolean enabled;
}
