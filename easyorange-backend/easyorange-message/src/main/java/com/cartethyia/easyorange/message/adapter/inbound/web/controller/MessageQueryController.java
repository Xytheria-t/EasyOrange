package com.cartethyia.easyorange.message.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.message.application.query.ConversationQueryHandler;
import com.cartethyia.easyorange.message.application.query.MessageQueryHandler;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息系统", description = "消息查询")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageQueryController {

    private final MessageQueryHandler queryHandler;
    private final ConversationQueryHandler conversationQueryHandler;

    @GetMapping("/conversations")
    public Result<List<ConversationListVO>> getConversations() {
        return Result.success(conversationQueryHandler.getConversations());
    }

    @GetMapping("/{id}")
    public Result<MessageVO> getMessageDetail(@PathVariable String id) {
        return Result.success(queryHandler.getMessageDetail(id));
    }

    @GetMapping("/list")
    public Result<PageResult<MessageVO>> getMyMessages(QueryMessageRequest request) {
        return Result.success(queryHandler.getMyMessages(toMessageQuery(request)));
    }

    @GetMapping("/unread")
    public Result<PageResult<MessageVO>> getUnreadMessages(QueryMessageRequest request) {
        return Result.success(queryHandler.getUnreadMessages(toMessageQuery(request)));
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        return Result.success(queryHandler.getUnreadCount());
    }

    @GetMapping("/conversation/{userId}")
    public Result<List<ConversationVO>> getConversation(@PathVariable String userId) {
        return Result.success(conversationQueryHandler.getConversation(userId));
    }

    /** 边界层 DTO → 领域查询参数，非法 isRead code 由 {@link ReadStatus#fromCode(String)} 抛 IllegalArgumentException 映射为 400。 */
    private static MessageQuery toMessageQuery(QueryMessageRequest request) {
        ReadStatus isRead = request.getIsRead() != null ? ReadStatus.fromCode(String.valueOf(request.getIsRead())) : null;
        return new MessageQuery(request.getPageNum(), request.getPageSize(), request.getType(), isRead);
    }
}