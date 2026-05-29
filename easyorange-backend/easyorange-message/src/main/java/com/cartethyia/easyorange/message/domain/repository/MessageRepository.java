package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.enums.ReadStatus;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<MessageAggregate> findById(Long id);

    List<MessageAggregate> findByReceiverId(Long receiverId, int limit);

    List<MessageAggregate> findByReceiverIdAndReadStatus(Long receiverId, ReadStatus readStatus, int limit);

    long countUnreadByReceiverId(Long receiverId);

    MessageAggregate save(MessageAggregate message);

    void update(MessageAggregate message);

    void delete(Long id);

    void markAllAsRead(Long receiverId);

    void markAsReadByType(Long receiverId, Integer type);
}
