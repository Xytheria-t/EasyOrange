package com.cartethyia.easyorange.order.domain.saga;

import java.util.Optional;

public interface SagaRepository {
    void save(SagaStatus sagaStatus);
    Optional<SagaStatus> findById(String sagaId);
    Optional<SagaStatus> findByOrderId(Long orderId);
    void update(SagaStatus sagaStatus);
}
