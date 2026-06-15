package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
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
    public MessageVO getMessageDetail(Long messageId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        MessageAggregate aggregate = queryRepository.findById(messageId);
        if (aggregate == null) {
            throw new MessageNotFoundException(messageId);
        }

        BizRequire.requireTrue(java.util.Objects.equals(aggregate.receiverId(), userId), MessageResultCode.MESSAGE_NOT_OWNER);

        return toMessageVO(aggregate, resolveUsernames(Set.of(aggregate)));
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<MessageAggregate> messagePage = queryRepository.findByReceiverId(request, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getUnreadMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<MessageAggregate> messagePage = queryRepository.findUnreadByReceiverId(request, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryRepository.countUnreadByReceiverId(userId);
    }

    private PageResult<MessageVO> toMessageVOPage(PageResult<MessageAggregate> messagePage) {
        Map<Long, String> usernameMap = resolveUsernames(
                messagePage.records().stream().collect(Collectors.toSet()));

        List<MessageVO> voList = messagePage.records().stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.total(),
                messagePage.current(), messagePage.size());
    }

    private Map<Long, String> resolveUsernames(Set<MessageAggregate> aggregates) {
        Set<Long> userIds = aggregates.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.senderId(), m.receiverId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userInfoPort.getUserInfoMap(userIds).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().username(),
                        (a, b) -> a
                ));
    }

    private MessageVO toMessageVO(MessageAggregate aggregate, Map<Long, String> usernameMap) {
        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .id(aggregate.id())
                .senderId(aggregate.senderId())
                .receiverId(aggregate.receiverId())
                .type(aggregate.type())
                .typeDesc(MessageType.getDescByCode(aggregate.type()))
                .title(aggregate.title())
                .content(aggregate.content())
                .isRead(aggregate.isRead())
                .readDesc(MessageStatus.getDescByCode(aggregate.isRead()))
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
