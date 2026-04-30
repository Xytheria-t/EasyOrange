package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.DomainEventConverter;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.DomainEventMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.DomainEventPO;
import com.cartethyia.easyorange.payment.domain.event.DomainEventStore;
import com.cartethyia.easyorange.payment.domain.event.StoredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcDomainEventStore implements DomainEventStore {

    private final DomainEventMapper domainEventMapper;

    @Override
    public void store(StoredEvent event) {
        DomainEventPO po = DomainEventConverter.toPO(event);
        domainEventMapper.insert(po);
    }

    @Override
    public List<StoredEvent> findUnpublished(int limit) {
        LambdaQueryWrapper<DomainEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DomainEventPO::getStatus, StoredEvent.STATUS_PENDING)
                .orderByAsc(DomainEventPO::getCreatedAt)
                .last("LIMIT " + limit);

        return domainEventMapper.selectList(wrapper).stream()
                .map(DomainEventConverter::toStoredEvent)
                .toList();
    }

    @Override
    public void markAsPublished(UUID eventId) {
        LambdaQueryWrapper<DomainEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DomainEventPO::getEventId, eventId);
        DomainEventPO po = domainEventMapper.selectOne(wrapper);
        if (po != null) {
            po.setStatus(StoredEvent.STATUS_PUBLISHED);
            po.setPublishedAt(java.time.Instant.now());
            domainEventMapper.updateById(po);
        }
    }
}
