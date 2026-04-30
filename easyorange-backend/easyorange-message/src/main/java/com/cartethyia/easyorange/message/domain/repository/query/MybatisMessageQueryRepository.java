package com.cartethyia.easyorange.message.domain.repository.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MybatisMessageQueryRepository implements MessageQueryRepository {

    private final MessageMapper messageMapper;

    @Override
    public Message findById(Long id) {
        return messageMapper.selectById(id);
    }

    @Override
    public PageResult<Message> findByReceiverId(QueryMessageRequest request, Long userId) {
        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId);

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }
        if (request.getIsRead() != null) {
            wrapper.eq(Message::getIsRead, request.getIsRead());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        return toPageResult(messagePage);
    }

    @Override
    public PageResult<Message> findUnreadByReceiverId(QueryMessageRequest request, Long userId) {
        PageRequest normalized = request.normalized();
        Page<Message> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode());

        if (request.getType() != null) {
            wrapper.eq(Message::getType, request.getType());
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        return toPageResult(messagePage);
    }

    @Override
    public UnreadCountVO countUnreadByReceiverId(Long userId) {
        List<Map<String, Object>> counts = messageMapper.countUnreadByType(userId, MessageStatus.UNREAD.getCode());

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

    private PageResult<Message> toPageResult(Page<Message> messagePage) {
        List<Message> records = messagePage.getRecords();
        return PageResult.of(records, messagePage.getTotal(),
                (int) messagePage.getCurrent(), (int) messagePage.getSize());
    }
}
