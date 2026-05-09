package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.output.UserInfoPort;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;
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

        Message message = queryRepository.findById(messageId);
        if (message == null) {
            throw new MessageNotFoundException(messageId);
        }

        BizRequire.eq(message.getReceiverId(), userId, MessageResultCode.MESSAGE_NOT_OWNER);

        return toMessageVO(message, resolveUsernames(Set.of(message)));
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<Message> messagePage = queryRepository.findByReceiverId(request, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getUnreadMessages(QueryMessageRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageResult<Message> messagePage = queryRepository.findUnreadByReceiverId(request, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryRepository.countUnreadByReceiverId(userId);
    }

    private PageResult<MessageVO> toMessageVOPage(PageResult<Message> messagePage) {
        Map<Long, String> usernameMap = resolveUsernames(
                messagePage.records().stream().collect(Collectors.toSet()));

        List<MessageVO> voList = messagePage.records().stream()
                .map(m -> toMessageVO(m, usernameMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, messagePage.total(),
                messagePage.current(), messagePage.size());
    }

    private Map<Long, String> resolveUsernames(Set<Message> messages) {
        Set<Long> userIds = messages.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getSenderId(), m.getReceiverId()))
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
                .readDesc(MessageStatus.getDescByCode(message.getIsRead()))
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
