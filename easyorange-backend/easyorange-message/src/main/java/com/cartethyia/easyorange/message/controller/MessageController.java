package com.cartethyia.easyorange.message.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取消息详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<MessageVO> getMessageDetail(@PathVariable Long id) {
        MessageVO message = messageService.getMessageDetail(id);
        return Result.success(message);
    }

    /**
     * 获取我的消息列表
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<MessageVO>> getMyMessages(QueryMessageRequest request) {
        PageResult<MessageVO> messages = messageService.getMyMessages(request);
        return Result.success(messages);
    }

    /**
     * 获取未读消息列表
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<MessageVO>> getUnreadMessages(QueryMessageRequest request) {
        PageResult<MessageVO> messages = messageService.getUnreadMessages(request);
        return Result.success(messages);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Result<UnreadCountVO> getUnreadCount() {
        UnreadCountVO count = messageService.getUnreadCount();
        return Result.success(count);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAllAsRead() {
        messageService.markAllAsRead();
        return Result.success();
    }

    /**
     * 批量标记消息为已读（新增）
     */
    @PutMapping("/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsReadBatch(@RequestBody List<Long> ids) {
        messageService.markAsReadBatch(ids);
        return Result.success();
    }

    /**
     * 按类型标记已读
     */
    @PutMapping("/read-by-type/{type}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsReadByType(@PathVariable Integer type) {
        BizRequire.notNull(MessageType.fromCode(type), "无效的消息类型");
        messageService.markAsReadByType(type);
        return Result.success();
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return Result.success();
    }
}
