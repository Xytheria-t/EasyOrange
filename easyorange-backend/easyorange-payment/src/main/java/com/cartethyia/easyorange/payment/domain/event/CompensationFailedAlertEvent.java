package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 补偿失败告警事件
 * 当支付/退款 Saga 补偿操作失败时发布，需要人工介入处理
 */
@Getter
public class CompensationFailedAlertEvent extends BaseDomainEvent {

    private final Long paymentId;
    private final String operationType; // "pay" 或 "refund"
    private final String errorMessage;
    private final String failureDetails;

    public CompensationFailedAlertEvent(Long paymentId, String operationType, String errorMessage, String failureDetails) {
        super();
        this.paymentId = paymentId;
        this.operationType = operationType;
        this.errorMessage = errorMessage;
        this.failureDetails = failureDetails;
    }
}