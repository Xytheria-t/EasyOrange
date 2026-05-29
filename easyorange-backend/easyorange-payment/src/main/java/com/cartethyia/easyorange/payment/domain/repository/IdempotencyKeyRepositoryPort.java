package com.cartethyia.easyorange.payment.domain.repository;

import com.cartethyia.easyorange.payment.domain.valueobject.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyKeyRepositoryPort {

    void save(IdempotencyKey key);

    Optional<IdempotencyKey> findByKey(String key);

    void updateResponse(String key, String responseData, String status);

    void deleteExpiredKeys();
}