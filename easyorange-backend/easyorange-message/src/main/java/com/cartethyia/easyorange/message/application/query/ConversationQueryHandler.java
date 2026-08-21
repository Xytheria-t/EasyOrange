package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationQueryHandler {

    /** 系统通知在会话列表中的固定占位会话 key（senderId 为 null 的消息归并到此处，避免 null key / NPE）。 */
    private static final String SYSTEM_CONVERSATION = "system";

    private final MessageQueryRepository queryRepository;
    private final UserInfoPort userInfoPort;

    @Transactional(readOnly = true)
    public List<ConversationVO> getConversation(String currentUserId, String otherUserId) {
        List<Message> messages = queryRepository.findConversation(currentUserId, otherUserId);
        if (messages.isEmpty()) {
            return List.of();
        }

        Map<String, UserInfo> userMap = userInfoPort.getUserInfoMap(Set.of(currentUserId, otherUserId));

        return messages.stream().map(msg -> toConversationVO(msg, userMap)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationListVO> getConversations(String currentUserId) {
        // 库端聚合：每个会话对方一条最新消息 + 按对方聚合的未读数，避免全量拉取用户所有消息
        List<Message> latestPerConversation = queryRepository.findLatestPerConversation(currentUserId);
        if (latestPerConversation.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> unreadCounts = queryRepository.countUnreadByConversation(currentUserId);

        Map<String, Message> latestByUser = new LinkedHashMap<>();
        for (Message msg : latestPerConversation) {
            latestByUser.putIfAbsent(otherUserId(currentUserId, msg), msg);
        }

        // 排除固定占位 key，避免向用户仓库查询不存在的 "system" 用户
        Set<String> userKeys = latestByUser.keySet().stream()
                .filter(key -> !SYSTEM_CONVERSATION.equals(key))
                .collect(Collectors.toSet());
        Map<String, UserInfo> userMap = userInfoPort.getUserInfoMap(userKeys);

        return latestByUser.entrySet().stream()
                .map(entry -> buildConversationListVO(entry.getKey(), entry.getValue(), userMap, unreadCounts))
                .toList();
    }

    /** 会话对方：senderId 为 null 的系统消息归并到固定 system 会话。 */
    private static String otherUserId(String currentUserId, Message msg) {
        if (msg.senderId() == null) {
            return SYSTEM_CONVERSATION;
        }
        return msg.senderId().equals(currentUserId) ? msg.receiverId() : msg.senderId();
    }

    private ConversationVO toConversationVO(Message message, Map<String, UserInfo> userMap) {
        UserInfo sender = message.senderId() != null ? userMap.get(message.senderId()) : null;
        UserInfo receiver = message.receiverId() != null ? userMap.get(message.receiverId()) : null;

        return ConversationVO.builder()
                .id(message.id())
                .senderId(message.senderId())
                .senderName(sender != null ? sender.username() : (message.senderId() == null ? "系统" : "未知用户"))
                .senderAvatar(sender != null ? sender.avatar() : null)
                .receiverId(message.receiverId())
                .receiverName(receiver != null ? receiver.username() : "未知用户")
                .receiverAvatar(receiver != null ? receiver.avatar() : null)
                .content(message.content())
                .isRead(Integer.valueOf(message.isRead().getCode()))
                .createTime(message.createTime())
                .build();
    }

    private ConversationListVO buildConversationListVO(
            String targetUserId, Message latestMsg, Map<String, UserInfo> userMap, Map<String, Integer> unreadCounts) {
        boolean isSystem = SYSTEM_CONVERSATION.equals(targetUserId);
        UserInfo targetUser = userMap.get(targetUserId);
        return ConversationListVO.builder()
                .targetUserId(targetUserId)
                .targetUserName(isSystem ? "系统通知" : (targetUser != null ? targetUser.username() : "未知用户"))
                .targetUserAvatar(isSystem ? null : (targetUser != null ? targetUser.avatar() : null))
                .lastMessage(latestMsg.content())
                .lastMessageTime(latestMsg.createTime())
                .unreadCount(unreadCounts.getOrDefault(targetUserId, 0))
                .build();
    }
}
