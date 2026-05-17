package com.cartethyia.easyorange.payment.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMetricsService 单元测试")
class PaymentMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private PaymentMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new PaymentMetricsService(meterRegistry);
    }

    @Nested
    @DisplayName("支付创建")
    class PaymentCreatedTests {

        @Test
        @DisplayName("记录支付创建指标")
        void recordPaymentCreated() {
            metricsService.recordPaymentCreated();

            Counter counter = meterRegistry.counter("payment.created.total", "type", "payment");
            assertThat(counter.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("支付成功")
    class PaymentSuccessTests {

        @Test
        @DisplayName("记录支付成功指标")
        void recordPaymentSuccess() {
            metricsService.recordPaymentSuccess();

            Counter counter = meterRegistry.counter("payment.success.total", "type", "payment");
            assertThat(counter.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("支付失败")
    class PaymentFailedTests {

        @Test
        @DisplayName("记录支付失败指标")
        void recordPaymentFailed() {
            metricsService.recordPaymentFailed();

            Counter counter = meterRegistry.counter("payment.failed.total", "type", "payment");
            assertThat(counter.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("退款")
    class RefundTests {

        @Test
        @DisplayName("记录退款指标")
        void recordRefund() {
            metricsService.recordRefund();

            Counter counter = meterRegistry.counter("payment.refund.total", "type", "refund");
            assertThat(counter.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("处理时间")
    class ProcessingTimeTests {

        @Test
        @DisplayName("记录支付处理时间")
        void recordPaymentProcessingTime() {
            metricsService.recordPaymentProcessingTime(100L);

            Timer timer = meterRegistry.timer("payment.processing.time");
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }

        @Test
        @DisplayName("启动和停止计时器")
        void startAndStopTimer() {
            Timer.Sample sample = metricsService.startTimer();

            assertThat(sample).isNotNull();

            metricsService.stopTimer(sample);

            Timer timer = meterRegistry.timer("payment.processing.time");
            assertThat(timer.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("并发冲突")
    class ConcurrentConflictTests {

        @Test
        @DisplayName("记录并发冲突指标")
        void recordConcurrentConflict() {
            metricsService.recordConcurrentConflict();

            Counter counter = meterRegistry.counter("payment.concurrent.conflict.total", "type", "concurrency");
            assertThat(counter.count()).isEqualTo(1);
        }
    }
}
