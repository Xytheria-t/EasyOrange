package com.cartethyia.easyorange.payment.domain.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    private String key;
    private Long userId;
    private String requestHash;
    private String responseData;
    private String status;
    private LocalDateTime expiresAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}
