package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.application.query.dto.MessageVO;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueryHandler {

    private final MessageQueryRepository queryRepository;
    private final UserInfoPort userInfoPort;

    @Transactional(readOnly = true)
    public PageResult<MessageVO> getMyMessages(String userId, MessageQuery query) {
        PageResult<Message> messagePage = queryRepository.findByReceiverId(query, userId);
        return toMessageVOPage(messagePage);
    }

    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount(String userId) {
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
        Map<String, UserInfo> userMap = resolveUserInfo(messagePage.records());
        return messagePage.map(m -> toMessageVO(m, userMap));
    }

    /** 一页消息涉及的收发方通常只有个位数，摊成一次 {@code IN} 查询，不做 N+1。 */
    private Map<String, UserInfo> resolveUserInfo(List<Message> aggregates) {
        Set<String> userIds = aggregates.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.senderId(), m.receiverId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoPort.getUserInfoMap(userIds);
    }

    private MessageVO toMessageVO(Message aggregate, Map<String, UserInfo> userMap) {
        UserInfo sender = aggregate.senderId() != null ? userMap.get(aggregate.senderId()) : null;
        UserInfo receiver = aggregate.receiverId() != null ? userMap.get(aggregate.receiverId()) : null;

        return MessageVO.builder()
                .id(aggregate.id())
                .senderId(aggregate.senderId())
                // senderId 为 null 即系统消息，与 ConversationQueryHandler 的占位口径一致
                .senderName(aggregate.senderId() == null ? "系统" : nameOf(sender))
                .senderAvatar(avatarOf(sender))
                .receiverId(aggregate.receiverId())
                .receiverName(nameOf(receiver))
                .receiverAvatar(avatarOf(receiver))
                .type(
                        aggregate.type() == null
                                ? null
                                : Integer.valueOf(aggregate.type().getCode()))
                .typeDesc(aggregate.type() == null ? null : aggregate.type().getDesc())
                .title(aggregate.title())
                .content(aggregate.content())
                .isRead(Integer.valueOf(aggregate.isRead().getCode()))
                .readDesc(ReadStatus.READ == aggregate.isRead() ? "已读" : "未读")
                .businessId(aggregate.businessId())
                // 点击跳转的目标页由 bizType 决定：前端不该靠标题中文猜商品还是订单
                .bizType(
                        aggregate.bizType() == null
                                ? null
                                : Integer.valueOf(aggregate.bizType().getCode()))
                .bizTypeDesc(
                        aggregate.bizType() == null ? null : aggregate.bizType().getDesc())
                .createTime(aggregate.createTime())
                .build();
    }

    private static String nameOf(UserInfo user) {
        return user != null ? user.username() : "未知用户";
    }

    private static String avatarOf(UserInfo user) {
        return user != null ? user.avatar() : null;
    }
}
