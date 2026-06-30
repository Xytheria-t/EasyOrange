package com.cartethyia.easyorange.message.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendSystemMessageCommand {

    private String receiverId;
    private String title;
    private String content;
    private String businessId;
}
