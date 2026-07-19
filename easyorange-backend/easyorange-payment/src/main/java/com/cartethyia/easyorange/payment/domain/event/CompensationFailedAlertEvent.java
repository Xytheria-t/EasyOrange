package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 补偿失败告警事件
 * 当支付/退款 Saga 补偿操作失败时发布，需要人工介入处理
 */
public record CompensationFailedAlertEvent(
        String paymentId,
        String operationType, // "pay" 或 "refund"
        String errorMessage,
        String failureDetails
) implements DomainEvent {
    @Override
    public String aggregateId() {
        return paymentId;
    }
}