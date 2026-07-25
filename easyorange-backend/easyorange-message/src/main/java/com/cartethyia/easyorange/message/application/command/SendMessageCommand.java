package com.cartethyia.easyorange.message.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageCommand(
        String receiverId,
        Integer type,
        @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String content,
        String businessId,
        String conversationId
) {}
