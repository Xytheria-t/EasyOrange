package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MybatisMessageQueryRepository extends BaseRepository<MessageMapper, Message> implements MessageQueryRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MybatisMessageQueryRepository(MessageMapper messageMapper, MessageDataMapper messageDataMapper) {
        super(messageMapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public MessageAggregate findById(String id) {
        return messageDataMapper.toAggregate(mapper.selectById(id));
    }

    @Override
    public PageResult<MessageAggregate> findByReceiverId(QueryMessageRequest request, String userId) {
        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        var wrapper = lambdaQuery();
        wrapper.eq(Message::getReceiverId, userId);

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }
        if (request.getIsRead() != null) {
            wrapper.eq(Message::getIsRead, request.getIsRead());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = wrapper.page(page);
        return toAggregatePageResult(messagePage);
    }

    @Override
    public PageResult<MessageAggregate> findUnreadByReceiverId(QueryMessageRequest request, String userId) {
        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        var wrapper = lambdaQuery();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode());

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = wrapper.page(page);
        return toAggregatePageResult(messagePage);
    }

    @Override
    public UnreadCountVO countUnreadByReceiverId(String userId) {
        List<Map<String, Object>> counts = mapper.countUnreadByType(userId, MessageStatus.UNREAD.getCode());

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

    private PageResult<MessageAggregate> toAggregatePageResult(Page<Message> messagePage) {
        List<MessageAggregate> records = messageDataMapper.toAggregateList(messagePage.getRecords());
        return PageResult.of(records, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }
}
