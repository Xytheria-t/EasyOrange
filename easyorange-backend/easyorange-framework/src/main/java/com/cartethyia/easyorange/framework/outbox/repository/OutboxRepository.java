package com.cartethyia.easyorange.framework.outbox.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.framework.outbox.converter.OutboxMessageConverter;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessagePO;
import com.cartethyia.easyorange.framework.outbox.mapper.OutboxMessageMapper;
import com.cartethyia.easyorange.framework.outbox.util.OutboxEventUtils;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OutboxRepository extends BaseRepository<OutboxMessageMapper, OutboxMessagePO> {

    public OutboxRepository(OutboxMessageMapper outboxMessageMapper) {
        super(outboxMessageMapper);
    }

    public void save(OutboxMessage message) {
        OutboxMessagePO po = OutboxMessageConverter.toPO(message);
        mapper.insert(po);
    }

    public List<OutboxMessage> findPending(int limit) {
        Page<OutboxMessagePO> page = lambdaQuery()
                .eq(OutboxMessagePO::getStatus, OutboxMessage.STATUS_PENDING)
                .orderByAsc(OutboxMessagePO::getCreatedAt)
                .page(new Page<>(1, limit));

        return page.getRecords().stream()
                .map(OutboxMessageConverter::toDomain)
                .toList();
    }

    public void markAsPublished(UUID eventId) {
        OutboxMessagePO po = lambdaQuery()
                .eq(OutboxMessagePO::getEventId, eventId)
                .one();
        if (po != null) {
            po.setStatus(OutboxMessage.STATUS_PUBLISHED);
            po.setPublishedAt(java.time.Instant.now());
            mapper.updateById(po);
        }
    }

    public void markAsFailed(UUID eventId, String errorMessage) {
        OutboxMessagePO po = lambdaQuery()
                .eq(OutboxMessagePO::getEventId, eventId)
                .one();
        if (po != null) {
            po.setStatus(OutboxMessage.STATUS_FAILED);
            po.setErrorMessage(OutboxEventUtils.truncate(errorMessage));
            mapper.updateById(po);
        }
    }
}