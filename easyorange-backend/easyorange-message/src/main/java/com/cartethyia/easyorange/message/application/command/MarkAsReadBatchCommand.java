package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadBatchCommand implements Command {

    private List<Long> messageIds;
}
