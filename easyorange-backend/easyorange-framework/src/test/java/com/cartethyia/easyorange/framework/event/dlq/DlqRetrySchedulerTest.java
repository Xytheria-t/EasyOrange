package com.cartethyia.easyorange.framework.event.dlq;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DlqRetryScheduler DLQ 分级重试调度器")
class DlqRetrySchedulerTest {

    private static final String DLQ_QUEUE = RabbitMQConfig.QUEUE_PRODUCT_CQRS + ".dlq";
    private static final String ORIGINAL_QUEUE = RabbitMQConfig.QUEUE_PRODUCT_CQRS;
    private static final String ROUTING_KEY = "product.created";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DlqRetryScheduler scheduler;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var metricsService = new EventMetricsService(meterRegistry);
        var strategy = new ExponentialBackoffRetryStrategy();
        scheduler = new DlqRetryScheduler(rabbitTemplate, strategy, metricsService);
    }

    // ───────────────────────── Test helpers ─────────────────────────

    private Message buildDlqMessage(int retryCount, String routingKey) {
        var props = new MessageProperties();
        if (routingKey != null) {
            props.setHeader("x-death", List.of(Map.of(
                    "queue", ORIGINAL_QUEUE,
                    "reason", "rejected",
                    "exchange", RabbitMQConfig.EXCHANGE_NAME,
                    "routing-keys", List.of(routingKey),
                    "count", 1L
            )));
        }
        if (retryCount > 0) {
            props.setHeader("x-retry-count", retryCount);
        }
        return new Message(new byte[0], props);
    }

    /** 模拟 receive 在第一次调用返回 message，之后返回 null（空队列） */
    private void mockReceiveOneMessage(Message message) {
        when(rabbitTemplate.receive(anyString(), anyLong()))
                .thenReturn(message)
                .thenReturn(null);
    }

    private void mockEmptyQueue() {
        when(rabbitTemplate.receive(anyString(), anyLong())).thenReturn(null);
    }

    // ───────────────────────── Tests ─────────────────────────

    @Nested
    @DisplayName("重投主队列")
    class RepublishToMainExchangeTests {

        @Test
        @DisplayName("retryCount=0 的消息重投到主交换，retryCount 递增为 1")
        void retryFromDlq_firstRetry_republishesWithIncrementedCount() {
            var message = buildDlqMessage(0, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            var msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                    eq(RabbitMQConfig.EXCHANGE_NAME),
                    eq(ROUTING_KEY),
                    msgCaptor.capture());
            Object retryCount = msgCaptor.getValue().getMessageProperties().getHeader("x-retry-count");
            assertThat(retryCount).isEqualTo(1);
        }

        @Test
        @DisplayName("retryCount=2 的消息仍可重试（未达上限 3）")
        void retryFromDlq_belowMax_republishes() {
            var message = buildDlqMessage(2, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(
                    eq(RabbitMQConfig.EXCHANGE_NAME),
                    eq(ROUTING_KEY),
                    any(Message.class));
        }

        @Test
        @DisplayName("重投后记录 retry 指标")
        void retryFromDlq_recordsRetryMetric() {
            var message = buildDlqMessage(0, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            var counter = meterRegistry.counter("easyorange.events.dlq",
                    "queue", ORIGINAL_QUEUE, "reason", "retry");
            org.junit.jupiter.api.Assertions.assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("转储 terminal 队列")
    class MoveToTerminalTests {

        @Test
        @DisplayName("retryCount=3 的消息转储 terminal 队列")
        void retryFromDlq_maxRetries_movesToTerminal() {
            var message = buildDlqMessage(3, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), any(Message.class));
            verify(rabbitTemplate, never()).send(
                    eq(RabbitMQConfig.EXCHANGE_NAME), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("无 x-death 头（无 routing key）的消息转储 terminal")
        void retryFromDlq_noRoutingKey_movesToTerminal() {
            var message = buildDlqMessage(0, null);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), any(Message.class));
        }

        @Test
        @DisplayName("转储时设置 x-terminal-reason 头")
        void retryFromDlq_maxRetries_setsTerminalReasonHeader() {
            var message = buildDlqMessage(3, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            var msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), msgCaptor.capture());
            Object terminalReason = msgCaptor.getValue().getMessageProperties().getHeader("x-terminal-reason");
            assertThat(terminalReason).isEqualTo("max-retries");
        }

        @Test
        @DisplayName("转储后记录 terminal 指标")
        void retryFromDlq_recordsTerminalMetric() {
            var message = buildDlqMessage(3, ROUTING_KEY);
            mockReceiveOneMessage(message);

            scheduler.retryFromDlq();

            var counter = meterRegistry.counter("easyorange.events.dlq",
                    "queue", ORIGINAL_QUEUE, "reason", "terminal_max_retries");
            org.junit.jupiter.api.Assertions.assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("空队列处理")
    class EmptyQueueTests {

        @Test
        @DisplayName("DLQ 为空时不发送任何消息")
        void retryFromDlq_emptyQueue_noSend() {
            mockEmptyQueue();

            scheduler.retryFromDlq();

            verify(rabbitTemplate, never()).send(anyString(), any(Message.class));
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }
    }

    @Nested
    @DisplayName("异常恢复")
    class ExceptionRecoveryTests {

        @Test
        @DisplayName("消息处理失败时重新投递到 DLQ")
        void retryFromDlq_processingFailure_republishesToDlq() {
            var message = buildDlqMessage(0, ROUTING_KEY);
            mockReceiveOneMessage(message);

            doThrow(new RuntimeException("Connection refused"))
                    .when(rabbitTemplate)
                    .send(eq(RabbitMQConfig.EXCHANGE_NAME),
                          eq(ROUTING_KEY),
                          any(Message.class));

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(
                    eq(RabbitMQConfig.DLQ_EXCHANGE_NAME),
                    eq(DLQ_QUEUE),
                    any(Message.class));
        }
    }

    @Nested
    @DisplayName("批处理上限")
    class BatchSizeTests {

        @Test
        @DisplayName("单个 DLQ 队列单次最多处理 10 条消息")
        void retryFromDlq_singleQueueBatchLimit() {
            var message = buildDlqMessage(0, ROUTING_KEY);
            // 模拟 receive 始终返回消息（不返回 null），BATCH_SIZE=10 应截断
            when(rabbitTemplate.receive(anyString(), anyLong())).thenReturn(message);

            scheduler.retryFromDlq();

            // 验证对主交换的 send 不超过 12 队列 × 10 条 = 120 次
            // 关键断言：即使 receive 始终返回消息，send 也被 BATCH_SIZE 限制
            verify(rabbitTemplate, atMost(120))
                    .send(eq(RabbitMQConfig.EXCHANGE_NAME), eq(ROUTING_KEY), any(Message.class));
        }
    }
}
