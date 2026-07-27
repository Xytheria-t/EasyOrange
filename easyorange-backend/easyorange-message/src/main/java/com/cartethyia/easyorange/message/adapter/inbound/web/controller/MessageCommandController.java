package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.message.application.command.DeleteMessageCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadBatchCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadCommand;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.RecallMessageCommand;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.websocket.TypingIndicatorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息系统", description = "消息发送/已读")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageCommandController {

    private final MessageCommandHandler commandHandler;
    private final TypingIndicatorService typingIndicatorService;

    @PostMapping
    public Result<Void> sendMessage(@RequestBody SendMessageCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }

    @PostMapping("/system")
    public Result<Void> sendSystemMessage(@RequestBody SendSystemMessageCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable String id) {
        commandHandler.handle(new MarkAsReadCommand(id));
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        commandHandler.handleMarkAllAsRead();
        return Result.success();
    }

    @PutMapping("/read")
    public Result<Void> markAsReadBatch(@RequestBody List<String> ids) {
        commandHandler.handle(new MarkAsReadBatchCommand(ids));
        return Result.success();
    }

    @PutMapping("/read-by-type/{type}")
    public Result<Void> markAsReadByType(@PathVariable Integer type) {
        if (MessageType.fromCode(String.valueOf(type)) == null) {
            return Result.error("无效的消息类型");
        }
        commandHandler.handleMarkAsReadByType(type);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable String id) {
        commandHandler.handle(new DeleteMessageCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/recall")
    public Result<Void> recallMessage(@PathVariable String id) {
        commandHandler.handle(new RecallMessageCommand(
                id,
                com.cartethyia.easyorange.framework.util.SecurityContextUtil.getCurrentUserIdOrThrow()
        ));
        return Result.success();
    }

    @PostMapping("/typing")
    public Result<Void> typing(@RequestBody java.util.Map<String, String> body) {
        String conversationId = body.get("conversationId");
        String targetUserId = body.get("targetUserId");
        typingIndicatorService.setTyping(conversationId, com.cartethyia.easyorange.framework.util.SecurityContextUtil.getCurrentUserIdOrThrow());
        return Result.success();
    }
}