package com.cartethyia.easyorange.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import com.cartethyia.easyorange.message.mapper.MessageMapper;
import com.cartethyia.easyorange.message.service.MessageService;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long receiverId, Integer type, String title, String content, Long businessId) {
        BizRequire.notNull(receiverId, "接收者 ID 不能为空");
        BizRequire.positive(receiverId, "接收者 ID 必须为正数");
        BizRequire.notNull(type, "消息类型不能为空");
        BizRequire.notBlank(title, "消息标题不能为空");
        BizRequire.notBlank(content, "消息内容不能为空");
        BizRequire.between(title.length(), 0, 200, "消息标题长度必须在 0-200 之间");
        BizRequire.between(content.length(), 0, 2000, "消息内容长度必须在 0-2000 之间");

        Long senderId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .title(org.springframework.web.util.HtmlUtils.htmlEscape(title))
                .content(org.springframework.web.util.HtmlUtils.htmlEscape(content))
                .isRead(ReadStatus.UNREAD.getCode())
                .businessId(businessId)
                .build();

        save(message);
        log.info("action=send_message messageId={} senderId={} receiverId={} type={}",
                message.getId(), senderId, receiverId, type);
        return toMessageVO(message, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendSystemMessage(Long receiverId, String title, String content) {
        BizRequire.notNull(receiverId, "接收者 ID 不能为空");
        BizRequire.positive(receiverId, "接收者 ID 必须为正数");
        BizRequire.notBlank(title, "消息标题不能为空");
        BizRequire.notBlank(content, "消息内容不能为空");
        BizRequire.between(title.length(), 0, 200, "消息标题长度必须在 0-200 之间");
        BizRequire.between(content.length(), 0, 2000, "消息内容长度必须在 0-2000 之间");

        Message message = Message.builder()
                .senderId(null)
                .receiverId(receiverId)
                .type(MessageType.SYSTEM.getCode())
                .title(org.springframework.web.util.HtmlUtils.htmlEscape(title))
                .content(org.springframework.web.util.HtmlUtils.htmlEscape(content))
                .isRead(ReadStatus.UNREAD.getCode())
                .build();

        save(message);
        log.info("action=send_system_message messageId={} receiverId={}", message.getId(), receiverId);
        return toMessageVO(message, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public MessageVO getMessageDetail(Long messageId) {
        Message message = validateMessageOwnership(messageId);
        return toMessageVO(message, resolveUsernames(Set.of(message)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(QueryMessageRequest request) {
        return queryMessages(request, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MessageVO> getUnreadMessages(QueryMessageRequest request) {
        return queryMessages(request, true);
    }

    private PageResult<MessageVO> queryMessages(QueryMessageRequest request, boolean unreadOnly) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Page<Message> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId);

        if (unreadOnly) {
            wrapper.eq(Message::getIsRead, ReadStatus.UNREAD.getCode());
        }
        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }
        if (request.getIsRead() != null) {
            wrapper.eq(Message::getIsRead, request.getIsRead());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = page(page, wrapper);
        List<Message> records = messagePage.getRecords();

        Map<Long, String> usernameMap = resolveUsernames(
                records.stream().collect(Collectors.toSet()));

        List<MessageVO> voList = records.stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        List<Map<String, Object>> counts = baseMapper.countUnreadByType(userId, ReadStatus.UNREAD.getCode());

        Map<Integer, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("type")).intValue(),
                        m -> ((Number) m.get("count")).longValue(),
                        (a, b) -> a));

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        return UnreadCountVO.builder()
                .total(total)
                .systemCount(countMap.getOrDefault(MessageType.SYSTEM.getCode(), 0L))
                .chatCount(countMap.getOrDefault(MessageType.CHAT.getCode(), 0L))
                .orderCount(countMap.getOrDefault(MessageType.ORDER.getCode(), 0L))
                .paymentCount(countMap.getOrDefault(MessageType.PAYMENT.getCode(), 0L))
                .activityCount(countMap.getOrDefault(MessageType.ACTIVITY.getCode(), 0L))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId) {
        Message message = getById(messageId);
        BizRequire.notNull(message, MessageResultCode.MESSAGE_NOT_FOUND);

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(message.getReceiverId(), userId, MessageResultCode.MESSAGE_NOT_OWNER);

        message.setIsRead(ReadStatus.READ.getCode());
        updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        lambdaUpdate()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, ReadStatus.UNREAD.getCode())
                .set(Message::getIsRead, ReadStatus.READ.getCode())
                .update();

        log.info("全部标记已读: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsReadBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        lambdaUpdate()
                .eq(Message::getReceiverId, userId)
                .in(Message::getId, ids)
                .eq(Message::getIsRead, ReadStatus.UNREAD.getCode())
                .set(Message::getIsRead, ReadStatus.READ.getCode())
                .update();

        log.info("action=mark_batch_read userId={} messageIds={}", userId, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsReadByType(Integer type) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        lambdaUpdate()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getType, type)
                .eq(Message::getIsRead, ReadStatus.UNREAD.getCode())
                .set(Message::getIsRead, ReadStatus.READ.getCode())
                .update();

        log.info("action=mark_type_read userId={} type={}", userId, type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId) {
        Message message = validateMessageOwnership(messageId);
        removeById(messageId);
    }

    private Message validateMessageOwnership(Long messageId) {
        Message message = getById(messageId);
        BizRequire.notNull(message, MessageResultCode.MESSAGE_NOT_FOUND);
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.eq(message.getReceiverId(), userId, MessageResultCode.MESSAGE_NOT_OWNER);
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> selectMessagePage(Page<Message> page, QueryMessageRequest request) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if (request.getSenderId() != null) {
            wrapper.eq(Message::getSenderId, request.getSenderId());
        }
        if (request.getReceiverId() != null) {
            wrapper.eq(Message::getReceiverId, request.getReceiverId());
        }
        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }
        if (request.getIsRead() != null) {
            wrapper.eq(Message::getIsRead, request.getIsRead());
        }
        wrapper.orderByDesc(Message::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }

    private Map<Long, String> resolveUsernames(Set<Message> messages) {
        Set<Long> userIds = messages.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getSenderId(), m.getReceiverId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private MessageVO toMessageVO(Message message, Map<Long, String> usernameMap) {
        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .type(message.getType())
                .typeDesc(MessageType.getDescByCode(message.getType()))
                .title(message.getTitle())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readDesc(ReadStatus.getDescByCode(message.getIsRead()))
                .businessId(message.getBusinessId())
                .createTime(message.getCreateTime())
                .updateTime(message.getUpdateTime());

        if (message.getSenderId() != null) {
            builder.senderName(usernameMap.getOrDefault(message.getSenderId(), "未知用户"));
        } else {
            builder.senderName("系统");
        }

        if (message.getReceiverId() != null) {
            builder.receiverName(usernameMap.getOrDefault(message.getReceiverId(), "未知用户"));
        }

        return builder.build();
    }
}
