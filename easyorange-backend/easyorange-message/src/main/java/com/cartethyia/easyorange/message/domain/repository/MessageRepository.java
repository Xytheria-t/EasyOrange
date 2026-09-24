package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<Message> findById(String id);

    Message save(Message message);

    void update(Message message);

    void markAsReadByType(String receiverId, Integer type);

    /**
     * 批量标记指定消息为已读 — 谓词（属于该接收者 + 仍为未读）与逐条
     * {@code aggregate.read(userId)} 的领域语义等价，下推到一条 SQL 避免 2N 次往返。
     * 非本人 / 不存在 / 已读的 ID 静默跳过。
     */
    void markAsReadByIds(String receiverId, List<String> messageIds);
}
