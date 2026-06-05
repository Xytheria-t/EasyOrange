package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class TemplateMessageRequest {

    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    private Map<String, String> variables;

    private Long businessId;
}
