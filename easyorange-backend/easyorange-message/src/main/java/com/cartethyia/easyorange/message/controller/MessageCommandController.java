package com.cartethyia.easyorange.message.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.message.application.command.DeleteMessageCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadBatchCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadCommand;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.message.enums.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageCommandController {

    private final MessageCommandHandler commandHandler;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> sendMessage(@RequestBody SendMessageCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }

    @PostMapping("/system")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> sendSystemMessage(@RequestBody SendSystemMessageCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsRead(@PathVariable Long id) {
        commandHandler.handle(MarkAsReadCommand.builder().messageId(id).build());
        return Result.success();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAllAsRead() {
        commandHandler.handleMarkAllAsRead();
        return Result.success();
    }

    @PutMapping("/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsReadBatch(@RequestBody List<Long> ids) {
        commandHandler.handle(MarkAsReadBatchCommand.builder().messageIds(ids).build());
        return Result.success();
    }

    @PutMapping("/read-by-type/{type}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsReadByType(@PathVariable Integer type) {
        if (MessageType.fromCode(type) == null) {
            return Result.error("无效的消息类型");
        }
        commandHandler.handleMarkAsReadByType(type);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        commandHandler.handle(DeleteMessageCommand.builder().messageId(id).build());
        return Result.success();
    }
}
