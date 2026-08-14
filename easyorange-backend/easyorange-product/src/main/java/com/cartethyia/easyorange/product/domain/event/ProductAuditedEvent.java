package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import java.time.LocalDateTime;

/**
 * 商品审核事件 — 审核结果使用领域枚举 {@link AuditAction}，替代字符串魔法值；
 * JSON 线上格式不变（{@code "action": "1"/"2"}，经 {@code @JsonValue} 编解码）。
 */
public record ProductAuditedEvent(
        String eventId,
        String productId,
        String productName,
        String sellerId,
        AuditAction action,
        String reason,
        LocalDateTime auditTime)
        implements ProductEvent {}
