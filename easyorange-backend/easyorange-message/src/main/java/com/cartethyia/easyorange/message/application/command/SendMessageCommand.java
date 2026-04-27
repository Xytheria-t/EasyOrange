package com.cartethyia.easyorange.message.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageCommand {

    private Long receiverId;
    private Integer type;
    private String title;
    private String content;
    private Long businessId;
}
