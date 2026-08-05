package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.domain.enums.MessageResultCode;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
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

    private final MessageQueryRepository queryRepository;
    private final UserInfoPort userInfoPort;

    @Transactional(readOnly = true)
    public MessageVO getMessageDetail(String messageId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message aggregate = queryRepository.findById(messageId);
        if (aggregate == null) {
            throw new MessageNotFoundException(messageId);
        }

        BizRequire.requireTrue(Objects.equals(aggregate.receiverId(), userId), MessageResultCode.MESSAGE_NOT_OWNER);

        return toMessageVO(aggregate, resolveUsernames(Set.of(aggregate)));
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(MessageQuery query) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<Message> messagePage = queryRepository.findByReceiverId(query, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getUnreadMessages(MessageQuery query) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<Message> messagePage = queryRepository.findUnreadByReceiverId(query, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        UnreadCount count = queryRepository.countUnreadByReceiverId(userId);
        return UnreadCountVO.builder()
                .total(count.total())
                .systemCount(count.systemCount())
                .chatCount(count.chatCount())
                .orderCount(count.orderCount())
                .paymentCount(count.paymentCount())
                .activityCount(count.activityCount())
                .build();
    }

    private PageResult<MessageVO> toMessageVOPage(PageResult<Message> messagePage) {
        Map<String, String> usernameMap = resolveUsernames(
                messagePage.records().stream().collect(Collectors.toSet()));

        List<MessageVO> voList = messagePage.records().stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.total(),
                messagePage.current(), messagePage.size());
    }

    private Map<String, String> resolveUsernames(Set<Message> aggregates) {
        Set<String> userIds = aggregates.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.senderId(), m.receiverId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new java.util.HashMap<>();
        for (var entry : userInfoPort.getUserInfoMap(userIds).entrySet()) {
            result.put(entry.getKey(), entry.getValue().username());
        }
        return result;
    }

    private MessageVO toMessageVO(Message aggregate, Map<String, String> usernameMap) {
        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .id(aggregate.id())
                .senderId(aggregate.senderId())
                .receiverId(aggregate.receiverId())
                .type(aggregate.type())
                .typeDesc(MessageType.getDescByCode(String.valueOf(aggregate.type())))
                .title(aggregate.title())
                .content(aggregate.content())
                .isRead(Integer.valueOf(aggregate.isRead().getCode()))
                .readDesc(ReadStatus.READ == aggregate.isRead() ? "已读" : "未读")
                .businessId(aggregate.businessId())
                .createTime(aggregate.createTime())
                .updateTime(null);

        if (aggregate.senderId() != null) {
            builder.senderName(usernameMap.getOrDefault(aggregate.senderId(), "未知用户"));
        } else {
            builder.senderName("系统");
        }

        if (aggregate.receiverId() != null) {
            builder.receiverName(usernameMap.getOrDefault(aggregate.receiverId(), "未知用户"));
        }

        return builder.build();
    }
}
