package com.cartethyia.easyorange.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SubscriptionRequest {

    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @NotBlank(message = "推送渠道不能为空")
    private String pushChannel;

    private Boolean enabled;
}
