package com.cartethyia.easyorange.message.adapter.outbound.persistence.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDataMapper;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.SystemConversation;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
@Transactional(readOnly = true)
public class MessageQueryRepositoryImpl extends BaseRepository<MessageMapper, MessageDO>
        implements MessageQueryRepository {

    public MessageQueryRepositoryImpl(MessageMapper messageMapper) {
        super(messageMapper);
    }

    @Override
    public Message findById(String id) {
        return MessageDataMapper.toAggregate(mapper.selectById(id));
    }

    @Override
    public PageResult<Message> findByReceiverId(MessageQuery query, String userId) {
        var pageReq = PageRequest.builder()
                .pageNum(query.pageNum())
                .pageSize(query.pageSize())
                .build();
        Page<MessageDO> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        var wrapper = lambdaQuery();
        wrapper.eq(MessageDO::getReceiverId, userId);

        if (query.type() != null) {
            wrapper.eq(MessageDO::getType, query.type());
        }
        if (query.isRead() != null) {
            wrapper.eq(MessageDO::getIsRead, query.isRead());
        }

        wrapper.orderByDesc(MessageDO::getCreateTime);

        Page<MessageDO> messagePage = wrapper.page(page);
        return toAggregatePageResult(messagePage);
    }

    @Override
    public UnreadCount countUnreadByReceiverId(String userId) {
        List<Map<String, Object>> counts =
                mapper.countUnreadByType(userId, Integer.valueOf(ReadStatus.UNREAD.getCode()));

        // eo_message.type 为 TINYINT，SQL 返回 Integer；用 Integer 作 key，按 MessageType.code 反查，
        // 避免 String code 与 Integer key 永不匹配导致按类型未读数恒为 0。
        Map<Integer, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("type")).intValue(),
                        m -> ((Number) m.get("count")).intValue(),
                        (a, b) -> a));

        return new UnreadCount(
                countMap.values().stream().mapToInt(Integer::intValue).sum(),
                countByCode(countMap, MessageType.SYSTEM),
                countByCode(countMap, MessageType.CHAT),
                countByCode(countMap, MessageType.ORDER),
                countByCode(countMap, MessageType.PAYMENT),
                countByCode(countMap, MessageType.ACTIVITY));
    }

    private static int countByCode(Map<Integer, Integer> countMap, MessageType type) {
        return countMap.getOrDefault(Integer.valueOf(type.getCode()), 0);
    }

    /** 会话详情窗口上限：只取最近 N 条（降序取页后反转为时间升序），避免长会话全量加载。 */
    private static final int CONVERSATION_HISTORY_LIMIT = 500;

    @Override
    public List<Message> findConversation(String userId, String otherUserId) {
        List<MessageDO> recent = isSystemConversation(otherUserId)
                ? lambdaQuery()
                        .isNull(MessageDO::getSenderId)
                        .eq(MessageDO::getReceiverId, userId)
                        .eq(MessageDO::getDelFlag, 0)
                        .orderByDesc(MessageDO::getCreateTime)
                        .page(new Page<>(1, CONVERSATION_HISTORY_LIMIT, false))
                        .getRecords()
                : lambdaQuery()
                        .and(w -> w.eq(MessageDO::getSenderId, userId)
                                .eq(MessageDO::getReceiverId, otherUserId)
                                .or()
                                .eq(MessageDO::getSenderId, otherUserId)
                                .eq(MessageDO::getReceiverId, userId))
                        .eq(MessageDO::getDelFlag, 0)
                        .orderByDesc(MessageDO::getCreateTime)
                        .page(new Page<>(1, CONVERSATION_HISTORY_LIMIT, false))
                        .getRecords();
        List<Message> chronological = new java.util.ArrayList<>(MessageDataMapper.toAggregateList(recent));
        java.util.Collections.reverse(chronological);
        return chronological;
    }

    /** 占位 ID 是给会话列表归并用的，不是真实用户；拿它当 sender_id 比会恒不成立。 */
    private static boolean isSystemConversation(String otherUserId) {
        return SystemConversation.ID.equals(otherUserId);
    }

    @Override
    public List<Message> findLatestPerConversation(String userId) {
        return MessageDataMapper.toAggregateList(mapper.selectLatestPerConversation(userId));
    }

    @Override
    public Map<String, Integer> countUnreadByConversation(String userId) {
        return mapper.countUnreadPerConversation(userId, Integer.valueOf(ReadStatus.UNREAD.getCode())).stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("other_id")),
                        row -> ((Number) row.get("cnt")).intValue(),
                        (a, b) -> a));
    }

    private PageResult<Message> toAggregatePageResult(Page<MessageDO> messagePage) {
        List<Message> records = MessageDataMapper.toAggregateList(messagePage.getRecords());
        return PageResult.of(
                records, messagePage.getTotal(), (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }
}
