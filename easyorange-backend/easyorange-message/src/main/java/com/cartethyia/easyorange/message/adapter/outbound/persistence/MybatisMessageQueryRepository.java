package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
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
    public PageResult<MessageAggregate> findByReceiverId(MessageQuery query, String userId) {
        var normalized = PageRequest.builder()
                .pageNum(query.pageNum())
                .pageSize(query.pageSize())
                .build();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        var wrapper = lambdaQuery();
        wrapper.eq(Message::getReceiverId, userId);

        if (query.type() != null) {
            wrapper.eq(Message::getType, query.type());
        }
        if (query.isRead() != null) {
            wrapper.eq(Message::getIsRead, query.isRead());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = wrapper.page(page);
        return toAggregatePageResult(messagePage);
    }

    @Override
    public PageResult<MessageAggregate> findUnreadByReceiverId(MessageQuery query, String userId) {
        var normalized = PageRequest.builder()
                .pageNum(query.pageNum())
                .pageSize(query.pageSize())
                .build();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        var wrapper = lambdaQuery();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode());

        if (query.type() != null) {
            wrapper.eq(Message::getType, query.type());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = wrapper.page(page);
        return toAggregatePageResult(messagePage);
    }

    @Override
    public UnreadCount countUnreadByReceiverId(String userId) {
        List<Map<String, Object>> counts = mapper.countUnreadByType(userId, MessageStatus.UNREAD.getCode());

        Map<Integer, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("type")).intValue(),
                        m -> ((Number) m.get("count")).longValue(),
                        (a, b) -> a));

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        return new UnreadCount(
                total,
                countMap.getOrDefault(MessageType.SYSTEM.getCode(), 0L),
                countMap.getOrDefault(MessageType.CHAT.getCode(), 0L),
                countMap.getOrDefault(MessageType.ORDER.getCode(), 0L),
                countMap.getOrDefault(MessageType.PAYMENT.getCode(), 0L),
                countMap.getOrDefault(MessageType.ACTIVITY.getCode(), 0L)
        );
    }

    private PageResult<MessageAggregate> toAggregatePageResult(Page<Message> messagePage) {
        List<MessageAggregate> records = messageDataMapper.toAggregateList(messagePage.getRecords());
        return PageResult.of(records, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }
}
