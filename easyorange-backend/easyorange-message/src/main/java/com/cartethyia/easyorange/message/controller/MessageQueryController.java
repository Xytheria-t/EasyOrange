package com.cartethyia.easyorange.message.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.message.application.query.ConversationQueryHandler;
import com.cartethyia.easyorange.message.application.query.MessageQueryHandler;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.ConversationVO;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageQueryController {

    private final MessageQueryHandler queryHandler;
    private final ConversationQueryHandler conversationQueryHandler;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<MessageVO> getMessageDetail(@PathVariable Long id) {
        return Result.success(queryHandler.getMessageDetail(id));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<MessageVO>> getMyMessages(QueryMessageRequest request) {
        return Result.success(queryHandler.getMyMessages(request));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<MessageVO>> getUnreadMessages(QueryMessageRequest request) {
        return Result.success(queryHandler.getUnreadMessages(request));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Result<UnreadCountVO> getUnreadCount() {
        return Result.success(queryHandler.getUnreadCount());
    }

    @GetMapping("/conversation/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<ConversationVO>> getConversation(@PathVariable Long userId) {
        return Result.success(conversationQueryHandler.getConversation(userId));
    }
}
