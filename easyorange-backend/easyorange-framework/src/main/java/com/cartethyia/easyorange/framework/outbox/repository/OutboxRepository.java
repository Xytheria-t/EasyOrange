package com.cartethyia.easyorange.framework.outbox.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.framework.outbox.converter.OutboxMessageConverter;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessagePO;
import com.cartethyia.easyorange.framework.outbox.mapper.OutboxMessageMapper;
import com.cartethyia.easyorange.framework.outbox.util.OutboxEventUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final OutboxMessageMapper outboxMessageMapper;

    public void save(OutboxMessage message) {
        OutboxMessagePO po = OutboxMessageConverter.toPO(message);
        outboxMessageMapper.insert(po);
    }

    public List<OutboxMessage> findPending(int limit) {
        LambdaQueryWrapper<OutboxMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxMessagePO::getStatus, OutboxMessage.STATUS_PENDING)
                .orderByAsc(OutboxMessagePO::getCreatedAt);

        Page<OutboxMessagePO> page = outboxMessageMapper.selectPage(new Page<>(1, limit), wrapper);

        return page.getRecords().stream()
                .map(OutboxMessageConverter::toDomain)
                .toList();
    }

    public void markAsPublished(UUID eventId) {
        LambdaQueryWrapper<OutboxMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxMessagePO::getEventId, eventId);
        OutboxMessagePO po = outboxMessageMapper.selectOne(wrapper);
        if (po != null) {
            po.setStatus(OutboxMessage.STATUS_PUBLISHED);
            po.setPublishedAt(java.time.Instant.now());
            outboxMessageMapper.updateById(po);
        }
    }

    public void markAsFailed(UUID eventId, String errorMessage) {
        LambdaQueryWrapper<OutboxMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxMessagePO::getEventId, eventId);
        OutboxMessagePO po = outboxMessageMapper.selectOne(wrapper);
        if (po != null) {
            po.setStatus(OutboxMessage.STATUS_FAILED);
            po.setErrorMessage(OutboxEventUtils.truncate(errorMessage));
            outboxMessageMapper.updateById(po);
        }
    }
}
