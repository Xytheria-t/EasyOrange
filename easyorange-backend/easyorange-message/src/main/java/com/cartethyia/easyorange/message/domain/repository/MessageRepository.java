package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<Message> findById(String id);

    List<Message> findByReceiverId(String receiverId, int limit);

    List<Message> findByReceiverIdAndReadStatus(String receiverId, ReadStatus readStatus, int limit);

    long countUnreadByReceiverId(String receiverId);

    Message save(Message message);

    void update(Message message);

    void delete(String id);

    void markAllAsRead(String receiverId);

    void markAsReadByType(String receiverId, Integer type);
}
