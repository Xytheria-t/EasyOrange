package com.cartethyia.easyorange.message.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallMessageCommand {

    private String messageId;
    private String operatorId;
}
