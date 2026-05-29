package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;

import java.util.List;

public interface MessageTemplateRepository {

    MessageTemplateAggregate findByCode(String templateCode);

    MessageTemplateAggregate save(MessageTemplateAggregate template);

    void update(MessageTemplateAggregate template);

    void deleteByIds(Long[] templateIds);

    List<MessageTemplateAggregate> findByCondition(MessageTemplateAggregate condition);

    boolean existsByCodeExcludingId(String templateCode, Long excludeId);
}
