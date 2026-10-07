package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.message.adapter.inbound.web.assembler.MessageAssembler;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.ConversationQueryHandler;
import com.cartethyia.easyorange.message.application.query.MessageQueryHandler;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息系统", description = "消息查询")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageQueryController {

    private final MessageQueryHandler queryHandler;
    private final ConversationQueryHandler conversationQueryHandler;
    private final MessageAssembler assembler;

    @GetMapping("/conversations")
    @Operation(summary = "会话列表：每个会话取对方最新一条消息 + 未读聚合数，系统通知归并到 system 会话")
    public Result<List<ConversationListVO>> getConversations(@AuthenticationPrincipal AuthUser user) {
        return Result.success(conversationQueryHandler.getConversations(user.userId()));
    }

    @GetMapping("/list")
    @Operation(summary = "收件箱分页（只含接收者为当前用户的消息），可按类型与已读状态过滤，时间倒序")
    public Result<PageResult<MessageVO>> getMyMessages(
            @AuthenticationPrincipal AuthUser user, QueryMessageRequest request) {
        return Result.success(queryHandler.getMyMessages(user.userId(), assembler.toMessageQuery(request)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "当前用户未读总数，并给出系统 / 聊天 / 订单 / 支付 / 活动分类计数")
    public Result<UnreadCountVO> getUnreadCount(@AuthenticationPrincipal AuthUser user) {
        return Result.success(queryHandler.getUnreadCount(user.userId()));
    }

    @GetMapping("/conversation/{userId}")
    @Operation(summary = "与指定用户的会话消息，时间升序、最多回溯最近 500 条；userId 传 system 取系统通知")
    public Result<List<ConversationVO>> getConversation(
            @AuthenticationPrincipal AuthUser user, @PathVariable String userId) {
        return Result.success(conversationQueryHandler.getConversation(user.userId(), userId));
    }
}
