package com.cartethyia.easyorange.message.application.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.port.output.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.dto.vo.ConversationVO;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationQueryHandler {

    private final MessageMapper messageMapper;
    private final UserInfoPort userInfoPort;

    @Transactional(readOnly = true)
    public List<ConversationVO> getConversation(Long otherUserId) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .eq(Message::getSenderId, currentUserId)
                        .eq(Message::getReceiverId, otherUserId)
                        .or()
                        .eq(Message::getSenderId, otherUserId)
                        .eq(Message::getReceiverId, currentUserId)
                )
                .eq(Message::getDelFlag, 0)
                .orderByAsc(Message::getCreateTime);

        List<Message> messages = messageMapper.selectList(wrapper);

        if (messages.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = new HashSet<>();
        userIds.add(currentUserId);
        userIds.add(otherUserId);

        Map<Long, UserInfo> userMap = userInfoPort.getUserInfoMap(userIds);

        return messages.stream()
                .map(m -> toConversationVO(m, userMap, currentUserId))
                .collect(Collectors.toList());
    }

    private ConversationVO toConversationVO(Message message, Map<Long, UserInfo> userMap, Long currentUserId) {
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
                .isRead(message.getIsRead())
                .createTime(message.getCreateTime())
                .build();
    }
}
