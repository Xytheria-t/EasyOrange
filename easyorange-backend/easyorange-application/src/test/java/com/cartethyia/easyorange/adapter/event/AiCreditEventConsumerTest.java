package com.cartethyia.easyorange.adapter.event;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.service.CreditScoringService;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.order.domain.event.OrderCompletedEvent;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCreditEventConsumer 单元测试")
class AiCreditEventConsumerTest {

    private static final String ORDER_ID = "100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String REPORTER_ID = "3";
    private static final String CONSUMER_ID = "AiCreditEventConsumer";

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private CreditScoringService creditScoringService;

    private AiCreditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new AiCreditEventConsumer(idempotencyChecker, metricsService, creditScoringService);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    private void mockClaimSuccess() {
        when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("onOrderCompleted")
    class OnOrderCompletedTests {

        @Test
        @DisplayName("交易完成后重算买卖双方信用分")
        void onOrderCompleted_shouldRecalculateBothParties() {
            mockClaimSuccess();

            var event = new OrderCompletedEvent("evt-1", ORDER_ID, BUYER_ID, SELLER_ID, List.of("200"));

            consumer.onOrderCompleted(event, buildMessage());

            verify(creditScoringService).recalculateScore(eq(BUYER_ID));
            verify(creditScoringService).recalculateScore(eq(SELLER_ID));
        }

        @Test
        @DisplayName("重复事件（领取处理权失败）不触发重算")
        void onOrderCompleted_withDuplicateEvent_shouldSkip() {
            when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(false);

            var event = new OrderCompletedEvent("evt-1", ORDER_ID, BUYER_ID, SELLER_ID, List.of("200"));

            consumer.onOrderCompleted(event, buildMessage());

            verify(creditScoringService, never()).recalculateScore(anyString());
        }

        @Test
        @DisplayName("旧版消息（无 buyerId/sellerId）降级跳过，不触发重算")
        void onOrderCompleted_withoutPartyIds_shouldSkip() {
            mockClaimSuccess();

            var event = new OrderCompletedEvent("evt-4", ORDER_ID, null, null, List.of("200"));

            consumer.onOrderCompleted(event, buildMessage());

            verify(creditScoringService, never()).recalculateScore(anyString());
        }
    }

    @Nested
    @DisplayName("onReportProcessed")
    class OnReportProcessedTests {

        @Test
        @DisplayName("举报确认后重算举报者信用分")
        void onReportProcessed_approved_shouldRecalculateReporter() {
            mockClaimSuccess();

            var event = new ReportProcessedEvent("evt-2", "rep-1", REPORTER_ID, "200", true, "属实", null);

            consumer.onReportProcessed(event, buildMessage());

            verify(creditScoringService).recalculateScore(eq(REPORTER_ID));
        }

        @Test
        @DisplayName("举报驳回不触发重算")
        void onReportProcessed_dismissed_shouldSkip() {
            mockClaimSuccess();

            var event = new ReportProcessedEvent("evt-3", "rep-1", REPORTER_ID, "200", false, "不属实", null);

            consumer.onReportProcessed(event, buildMessage());

            verify(creditScoringService, never()).recalculateScore(anyString());
        }
    }
}
