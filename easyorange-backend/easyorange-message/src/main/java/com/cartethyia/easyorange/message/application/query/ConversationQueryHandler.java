package com.cartethyia.easyorange.message.application.query;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationQueryHandler {

    private final MessageMapper messageMapper;
    private final UserInfoPort userInfoPort;

    @Transactional(readOnly = true)
    public List<ConversationVO> getConversation(String otherUserId) {
        String currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();

        List<MessageDO> messages = ChainWrappers.lambdaQueryChain(messageMapper)
                .and(w -> w
                        .eq(MessageDO::getSenderId, currentUserId).eq(MessageDO::getReceiverId, otherUserId)
                        .or()
                        .eq(MessageDO::getSenderId, otherUserId).eq(MessageDO::getReceiverId, currentUserId)
                )
                .eq(MessageDO::getDelFlag, 0)
                .orderByAsc(MessageDO::getCreateTime)
                .list();

        if (messages.isEmpty()) {
            return List.of();
        }

        Map<String, UserInfo> userMap = userInfoPort.getUserInfoMap(Set.of(currentUserId, otherUserId));

        return messages.stream()
                .map(msg -> toConversationVO(msg, userMap))
                .toList();
    }

    private ConversationVO toConversationVO(MessageDO message, Map<String, UserInfo> userMap) {
        UserInfo sender = userMap.get(message.getSenderId());
        UserInfo receiver = userMap.get(message.getReceiverId());

        return ConversationVO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.username() : "未知用户")
                .senderAvatar(sender != null ? sender.avatar() : null)
                .receiverId(message.getReceiverId())
                .receiverName(receiver != null ? receiver.username() : "未知用户")
                .receiverAvatar(receiver != null ? receiver.avatar() : null)
                .content(message.getContent())
                .isRead(Integer.valueOf(message.getIsRead().getCode()))
                .createTime(message.getCreateTime())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConversationListVO> getConversations() {
        String currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();

        List<MessageDO> messages = ChainWrappers.lambdaQueryChain(messageMapper)
                .and(w -> w
                        .eq(MessageDO::getSenderId, currentUserId)
                        .or()
                        .eq(MessageDO::getReceiverId, currentUserId)
                )
                .eq(MessageDO::getDelFlag, 0)
                .orderByDesc(MessageDO::getCreateTime)
                .list();

        if (messages.isEmpty()) {
            return List.of();
        }

        Map<String, MessageDO> latestByUser = new LinkedHashMap<>();
        Map<String, Integer> unreadCounts = new HashMap<>();

        for (MessageDO msg : messages) {
            String otherUserId = msg.getSenderId().equals(currentUserId) ? msg.getReceiverId() : msg.getSenderId();
            latestByUser.putIfAbsent(otherUserId, msg);
            if (msg.getReceiverId().equals(currentUserId) && ReadStatus.UNREAD == msg.getIsRead()) {
                unreadCounts.merge(otherUserId, 1, Integer::sum);
            }
        }

        Map<String, UserInfo> userMap = userInfoPort.getUserInfoMap(latestByUser.keySet());
        userMap.put(currentUserId, userMap.get(currentUserId));

        return latestByUser.entrySet().stream()
                .map(entry -> buildConversationListVO(entry.getKey(), entry.getValue(), userMap, unreadCounts))
                .toList();
    }

    private ConversationListVO buildConversationListVO(String targetUserId, MessageDO latestMsg,
                                                        Map<String, UserInfo> userMap, Map<String, Integer> unreadCounts) {
        UserInfo targetUser = userMap.get(targetUserId);
        return ConversationListVO.builder()
                .targetUserId(targetUserId)
                .targetUserName(targetUser != null ? targetUser.username() : "未知用户")
                .targetUserAvatar(targetUser != null ? targetUser.avatar() : null)
                .lastMessage(latestMsg.getContent())
                .lastMessageTime(latestMsg.getCreateTime())
                .unreadCount(unreadCounts.getOrDefault(targetUserId, 0))
                .build();
    }
}
