package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.entity.MessageTemplate;

import java.util.List;

public interface MessageTemplateRepository {

    MessageTemplate findByCode(String templateCode);

    MessageTemplate save(MessageTemplate template);

    void update(MessageTemplate template);

    void deleteByIds(Long[] templateIds);

    List<MessageTemplate> findByCondition(MessageTemplate condition);

    boolean existsByCodeExcludingId(String templateCode, Long excludeId);
}
