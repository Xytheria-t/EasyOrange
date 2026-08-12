package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.ConversationQueryHandler;
import com.cartethyia.easyorange.message.application.query.MessageQueryHandler;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
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

    @GetMapping("/conversations")
    public Result<List<ConversationListVO>> getConversations(@AuthenticationPrincipal AuthUser user) {
        return Result.success(conversationQueryHandler.getConversations(user.userId()));
    }

    @GetMapping("/{id}")
    public Result<MessageVO> getMessageDetail(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return Result.success(queryHandler.getMessageDetail(user.userId(), id));
    }

    @GetMapping("/list")
    public Result<PageResult<MessageVO>> getMyMessages(
            @AuthenticationPrincipal AuthUser user, QueryMessageRequest request) {
        return Result.success(queryHandler.getMyMessages(user.userId(), toMessageQuery(request)));
    }

    @GetMapping("/unread")
    public Result<PageResult<MessageVO>> getUnreadMessages(
            @AuthenticationPrincipal AuthUser user, QueryMessageRequest request) {
        return Result.success(queryHandler.getUnreadMessages(user.userId(), toMessageQuery(request)));
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount(@AuthenticationPrincipal AuthUser user) {
        return Result.success(queryHandler.getUnreadCount(user.userId()));
    }

    @GetMapping("/conversation/{userId}")
    public Result<List<ConversationVO>> getConversation(
            @AuthenticationPrincipal AuthUser user, @PathVariable String userId) {
        return Result.success(conversationQueryHandler.getConversation(user.userId(), userId));
    }

    /** 边界层 DTO → 领域查询参数，非法 isRead code 由 {@link ReadStatus#fromCode(String)} 抛 IllegalArgumentException 映射为 400。 */
    private static MessageQuery toMessageQuery(QueryMessageRequest request) {
        ReadStatus isRead =
                request.getIsRead() != null ? ReadStatus.fromCode(String.valueOf(request.getIsRead())) : null;
        return new MessageQuery(request.getPageNum(), request.getPageSize(), request.getType(), isRead);
    }
}
