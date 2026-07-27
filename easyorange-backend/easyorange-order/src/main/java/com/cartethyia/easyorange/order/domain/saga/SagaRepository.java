package com.cartethyia.easyorange.order.domain.saga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SagaRepository {
    void save(SagaStatus sagaStatus);
    Optional<SagaStatus> findById(String sagaId);
    Optional<SagaStatus> findByOrderId(Long orderId);
    void update(SagaStatus sagaStatus);

    /**
     * 查找超时的活跃 saga — 状态为 PENDING 或 COMPENSATING 且 updatedAt 早于阈值。
     *
     * @param threshold 超时阈值（updatedAt < threshold 的 saga 被视为超时）
     * @return 超时 saga 列表
     */
    List<SagaStatus> findTimedOut(LocalDateTime threshold);
}
