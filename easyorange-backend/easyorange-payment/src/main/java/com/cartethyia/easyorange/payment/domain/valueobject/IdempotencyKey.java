package com.cartethyia.easyorange.payment.domain.valueobject;

import java.time.LocalDateTime;

public record IdempotencyKey(
        String key,
        String userId,
        String requestHash,
        String responseData,
        String status,
        LocalDateTime expiresAt
) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public static IdempotencyKey of(String key, String userId, String requestHash, String status, LocalDateTime expiresAt) {
        return new IdempotencyKey(key, userId, requestHash, null, status, expiresAt);
    }
}
