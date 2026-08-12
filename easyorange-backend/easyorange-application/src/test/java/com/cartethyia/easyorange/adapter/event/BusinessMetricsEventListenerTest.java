package com.cartethyia.easyorange.adapter.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.product.domain.event.ProductPutOnlineEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessMetricsEventListener 测试")
class BusinessMetricsEventListenerTest {

    @Test
    @DisplayName("业务事件驱动计数器递增")
    void businessEvents_incrementCounters() {
        var meterRegistry = new SimpleMeterRegistry();
        var listener = new BusinessMetricsEventListener(new BusinessMetricsService(meterRegistry));

        listener.onUserRegistered(new UserRegisteredEvent("1", "alice"));
        listener.onUserRegistered(new UserRegisteredEvent("2", "bob"));
        listener.onProductPutOnline(new ProductPutOnlineEvent("p1", "seller1"));
        listener.onOrderCreated(new OrderCreatedEvent("o1", "buyer1", "seller1", List.of(), BigDecimal.ONE));
        listener.onPaymentSucceeded(new PaymentSucceededEvent("pay1", "tx1"));

        assertThat(meterRegistry.counter("easyorange.users.registered").count()).isEqualTo(2);
        assertThat(meterRegistry.counter("easyorange.products.published").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("easyorange.orders.created").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("easyorange.payments.completed").count()).isEqualTo(1);
    }
}
