package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.message.adapter.inbound.web.assembler.MessageAssembler;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.SendMessageRequest;
import com.cartethyia.easyorange.message.application.command.MarkAsReadBatchCommand;
import com.cartethyia.easyorange.message.application.command.MarkAsReadCommand;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.RecallMessageCommand;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.Operation;
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
    private final MessageAssembler assembler;

    @PostMapping
    @Operation(summary = "发送消息：敏感词过滤后落库，限流 5 条/秒/用户（Redis 不可用放行），会话 ID 由后端算出")
    public Result<Void> sendMessage(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody SendMessageRequest request) {
        commandHandler.sendMessage(user.userId(), assembler.toSendCommand(request));
        return Result.success();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读：仅接收者有权（非接收者报错），已读再标幂等且不改读取时间")
    public Result<Void> markAsRead(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.markAsRead(user.userId(), new MarkAsReadCommand(id));
        return Result.success();
    }

    @PutMapping("/read")
    @Operation(summary = "批量标记已读（单次最多 50 条）：非本人 / 不存在 / 已读的 ID 静默跳过，空列表按成功返回")
    public Result<Void> markAsReadBatch(@AuthenticationPrincipal AuthUser user, @RequestBody List<String> ids) {
        commandHandler.markAsReadBatch(user.userId(), new MarkAsReadBatchCommand(ids));
        return Result.success();
    }

    @PutMapping("/read-by-type/{type}")
    @Operation(summary = "按消息类型把当前用户该类全部未读标记为已读；type 非法由枚举解析拒绝")
    public Result<Void> markAsReadByType(@AuthenticationPrincipal AuthUser user, @PathVariable Integer type) {
        // 非法类型由 fromCode 抛 IllegalArgumentException → 全局异常处理器映射为 400
        MessageType.fromCode(String.valueOf(type));
        commandHandler.markAsReadByType(user.userId(), type);
        return Result.success();
    }

    @PutMapping("/{id}/recall")
    @Operation(summary = "撤回消息：仅发送者可撤、限 2 分钟窗口且不可重复；落库后发事件供定向广播")
    public Result<Void> recallMessage(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.recallMessage(user.userId(), new RecallMessageCommand(id));
        return Result.success();
    }
}
