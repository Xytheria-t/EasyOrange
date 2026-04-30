package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.ReadStatus;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<Message> findById(Long id);

    List<Message> findByReceiverId(Long receiverId, int limit);

    List<Message> findByReceiverIdAndReadStatus(Long receiverId, ReadStatus readStatus, int limit);

    long countUnreadByReceiverId(Long receiverId);

    void save(Message message);

    void update(Message message);

    void delete(Long id);

    void markAllAsRead(Long receiverId);

    void markAsReadByType(Long receiverId, Integer type);
}
