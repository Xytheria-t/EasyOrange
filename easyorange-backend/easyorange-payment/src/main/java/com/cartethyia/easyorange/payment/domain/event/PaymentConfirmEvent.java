package com.cartethyia.easyorange.payment.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

/**
 * 支付确认事件密封接口。
 * <p>
 * permits 子句列出 confirmPay 的两种可能结果，编译器确保 switch 穷尽性。
 */
public sealed interface PaymentConfirmEvent extends DomainEvent
    permits PaymentSucceededEvent, PaymentFailedEvent {
}
