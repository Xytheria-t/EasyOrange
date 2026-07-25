package com.cartethyia.easyorange.message.application.command;

public record SendSystemMessageCommand(
        String receiverId,
        String title,
        String content,
        String businessId
) {}
