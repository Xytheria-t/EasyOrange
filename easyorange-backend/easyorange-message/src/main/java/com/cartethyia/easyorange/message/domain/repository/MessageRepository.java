package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import java.util.Optional;

public interface MessageRepository {

    Optional<Message> findById(String id);

    Message save(Message message);

    void update(Message message);

    void delete(String id);

    void markAllAsRead(String receiverId);

    void markAsReadByType(String receiverId, Integer type);
}
