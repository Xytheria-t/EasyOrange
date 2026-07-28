package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplate;

import java.util.List;

public interface MessageTemplateRepository {

    MessageTemplate findByCode(String templateCode);

    MessageTemplate save(MessageTemplate template);

    void update(MessageTemplate template);

    void deleteByIds(String[] templateIds);

    List<MessageTemplate> findByCondition(MessageTemplate condition);

    boolean existsByCodeExcludingId(String templateCode, String excludeId);
}
