package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.message.application.command.MarkAsReadBatchCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadCommand;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.RecallMessageCommand;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息系统", description = "消息发送/已读")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageCommandController {

    private final MessageCommandHandler commandHandler;

    @PostMapping
    public Result<Void> sendMessage(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody SendMessageCommand command) {
        commandHandler.sendMessage(user.userId(), command);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.markAsRead(user.userId(), new MarkAsReadCommand(id));
        return Result.success();
    }

    @PutMapping("/read")
    public Result<Void> markAsReadBatch(@AuthenticationPrincipal AuthUser user, @RequestBody List<String> ids) {
        commandHandler.markAsReadBatch(user.userId(), new MarkAsReadBatchCommand(ids));
        return Result.success();
    }

    @PutMapping("/read-by-type/{type}")
    public Result<Void> markAsReadByType(@AuthenticationPrincipal AuthUser user, @PathVariable Integer type) {
        // 非法类型由 fromCode 抛 IllegalArgumentException → 全局异常处理器映射为 400
        MessageType.fromCode(String.valueOf(type));
        commandHandler.markAsReadByType(user.userId(), type);
        return Result.success();
    }

    @PutMapping("/{id}/recall")
    public Result<Void> recallMessage(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.recallMessage(user.userId(), new RecallMessageCommand(id));
        return Result.success();
    }
}
