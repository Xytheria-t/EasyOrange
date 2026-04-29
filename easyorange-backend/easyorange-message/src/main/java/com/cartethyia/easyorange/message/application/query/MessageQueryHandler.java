package com.cartethyia.easyorange.message.application.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import com.cartethyia.easyorange.message.mapper.MessageMapper;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.service.user.UserService;
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
public class MessageQueryHandler {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final UserService userService;

    @Transactional(readOnly = true)
    public MessageVO getMessageDetail(Long messageId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        BizRequire.eq(message.getReceiverId(), userId, "无权查看此消息");

        return toMessageVO(message, resolveUsernames(Set.of(message)));
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId);

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }
        if (request.getIsRead() != null) {
            wrapper.eq(Message::getIsRead, request.getIsRead());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        List<Message> records = messagePage.getRecords();

        Map<Long, String> usernameMap = resolveUsernames(
                records.stream().collect(Collectors.toSet()));

        List<MessageVO> voList = records.stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getUnreadMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, ReadStatus.UNREAD.getCode());

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        List<Message> records = messagePage.getRecords();

        Map<Long, String> usernameMap = resolveUsernames(
                records.stream().collect(Collectors.toSet()));

        List<MessageVO> voList = records.stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }

    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        List<Map<String, Object>> counts = messageMapper.countUnreadByType(userId, ReadStatus.UNREAD.getCode());

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
