package com.cartethyia.easyorange.adapter.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessMetricsEventListener 测试")
class BusinessMetricsEventListenerTest {

    @Test
    @DisplayName("业务事件驱动计数器递增")
    void businessEvents_incrementCounters() {
        var meterRegistry = new SimpleMeterRegistry();
        var listener = new BusinessMetricsEventListener(new BusinessMetricsService(meterRegistry));

        listener.onUserRegistered();
        listener.onUserRegistered();
        listener.onProductPutOnline();
        listener.onOrderCreated();
        listener.onPaymentSucceeded();

        assertThat(meterRegistry.counter("easyorange.users.registered").count()).isEqualTo(2);
        assertThat(meterRegistry.counter("easyorange.products.published").count())
                .isEqualTo(1);
        assertThat(meterRegistry.counter("easyorange.orders.created").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("easyorange.payments.completed").count())
                .isEqualTo(1);
    }
}
