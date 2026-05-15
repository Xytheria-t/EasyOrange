package com.cartethyia.easyorange.message.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;

    private Long businessId;

    private String conversationId;
}
