package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteMessageCommand implements Command {

    private Long messageId;
}
