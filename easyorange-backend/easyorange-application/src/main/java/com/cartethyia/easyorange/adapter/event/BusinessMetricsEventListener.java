package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.product.domain.event.ProductPutOnlineEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 业务指标同步监听器 — 与发布者同事务、同线程，将关键领域事件映射为 Prometheus 计数器。
 * <p>
 * 与 {@code ProductDomainEventListener} 同一模式：领域事件经 Spring Modulith
 * 发布器同步派发到本监听器，指标与业务动作同事务生效。
 */
@Component
@RequiredArgsConstructor
public class BusinessMetricsEventListener {

    private final BusinessMetricsService businessMetricsService;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        businessMetricsService.incrementUserRegistration();
    }

    @EventListener
    public void onProductPutOnline(ProductPutOnlineEvent event) {
        businessMetricsService.incrementProductPublished();
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        businessMetricsService.incrementOrderCreated();
    }

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        businessMetricsService.incrementPaymentCompleted();
    }
}
