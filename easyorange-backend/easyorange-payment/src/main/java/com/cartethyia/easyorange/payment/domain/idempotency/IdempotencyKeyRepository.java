package com.cartethyia.easyorange.payment.domain.idempotency;

import java.util.Optional;

public interface IdempotencyKeyRepository {

    void save(IdempotencyKey key);

    Optional<IdempotencyKey> findByKey(String key);

    void updateResponse(String key, String responseData, String status);

    void deleteExpiredKeys();
}
