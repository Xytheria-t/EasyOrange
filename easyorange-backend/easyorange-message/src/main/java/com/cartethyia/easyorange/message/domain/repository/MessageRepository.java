package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.enums.ReadStatus;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<MessageAggregate> findById(String id);

    List<MessageAggregate> findByReceiverId(String receiverId, int limit);

    List<MessageAggregate> findByReceiverIdAndReadStatus(String receiverId, ReadStatus readStatus, int limit);

    long countUnreadByReceiverId(String receiverId);

    MessageAggregate save(MessageAggregate message);

    void update(MessageAggregate message);

    void delete(String id);

    void markAllAsRead(String receiverId);

    void markAsReadByType(String receiverId, Integer type);
}
