package com.cartethyia.easyorange.framework.event.dlq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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

    /** 捕获最近一次 {@code execute} 调用时创建的 Channel mock，用于断言 ack/nack 行为 */
    private final Channel[] channelHolder = new Channel[1];

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var metricsService = new EventMetricsService(meterRegistry);
        scheduler = new DlqRetryScheduler(rabbitTemplate, metricsService);
    }

    // ───────────────────────── Test helpers ─────────────────────────

    private GetResponse buildGetResponse(long deliveryTag, int retryCount, String routingKey, Long deathTimeMillis) {
        Map<String, Object> headers = new HashMap<>();
        if (routingKey != null) {
            Map<String, Object> death = new HashMap<>();
            death.put("queue", ORIGINAL_QUEUE);
            death.put("reason", "rejected");
            death.put("exchange", RabbitMQConfig.EXCHANGE_NAME);
            death.put("routing-keys", List.of(routingKey));
            death.put("count", 1L);
            if (deathTimeMillis != null) {
                death.put("time", deathTimeMillis);
            }
            headers.put("x-death", List.of(death));
        }
        if (retryCount > 0) {
            headers.put("x-retry-count", retryCount);
        }
        var props = new AMQP.BasicProperties.Builder().headers(headers).build();
        var envelope = new Envelope(deliveryTag, false, RabbitMQConfig.EXCHANGE_NAME, routingKey);
        return new GetResponse(envelope, props, new byte[0], 1);
    }

    /**
     * 模拟 DLQ 队列的 {@code execute}：用 mock Channel 驱动调度器回调，
     * {@code basicGet} 按序返回给定响应，取完后返回 null（空队列）。
     * <p>
     * 响应计数跨队列共享（与旧 receive 桩语义一致）：首条消息只被第一个队列拉取到，
     * 其余队列视为空，避免 10 个队列各处理一遍同一条消息。
     */
    private void mockQueue(GetResponse... responses) {
        AtomicInteger index = new AtomicInteger();
        when(rabbitTemplate.execute(any(ChannelCallback.class)))
                .thenAnswer(inv -> {
                    Channel channel = mock(Channel.class);
                    if (channelHolder[0] == null) {
                        // 只捕获实际拉取到消息的（第一个）channel，用于断言 ack/nack
                        channelHolder[0] = channel;
                    }
                    when(channel.basicGet(anyString(), eq(false)))
                            .thenAnswer(inv2 -> {
                                int i = index.getAndIncrement();
                                return i < responses.length ? responses[i] : null;
                            });
                    return ((ChannelCallback<?>) inv.getArgument(0)).doInRabbit(channel);
                });
    }

    private void mockEmptyQueue() {
        mockQueue();
    }

    // ───────────────────────── Tests ─────────────────────────

    @Nested
    @DisplayName("重投主队列")
    class RepublishToMainExchangeTests {

        @Test
        @DisplayName("退避到期（retryCount=0）的消息重投主交换，retryCount 递增为 1 并 ack")
        void retryFromDlq_firstRetry_republishesWithIncrementedCount() throws Exception {
            long deliveryTag = 1L;
            mockQueue(buildGetResponse(deliveryTag, 0, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            scheduler.retryFromDlq();

            var msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(eq(RabbitMQConfig.EXCHANGE_NAME), eq(ROUTING_KEY), msgCaptor.capture());
            Object retryCount = msgCaptor.getValue().getMessageProperties().getHeader("x-retry-count");
            assertThat(retryCount).isEqualTo(1);
            verify(channelHolder[0]).basicAck(deliveryTag, false);
        }

        @Test
        @DisplayName("退避到期（retryCount=2，已满 15min）的消息仍可重试（未达上限 3）")
        void retryFromDlq_belowMax_republishes() {
            mockQueue(buildGetResponse(1L, 2, ROUTING_KEY, System.currentTimeMillis() - 20 * 60_000));

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(eq(RabbitMQConfig.EXCHANGE_NAME), eq(ROUTING_KEY), any(Message.class));
        }

        @Test
        @DisplayName("重投后记录 retry 指标")
        void retryFromDlq_recordsRetryMetric() {
            mockQueue(buildGetResponse(1L, 0, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            scheduler.retryFromDlq();

            var counter = meterRegistry.counter("easyorange.events.dlq", "queue", ORIGINAL_QUEUE, "reason", "retry");
            org.junit.jupiter.api.Assertions.assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("退避未到期")
    class BackoffWaitTests {

        @Test
        @DisplayName("retryCount=0 刚进 DLQ（未满 1min）的消息不重投，nack 回队等待")
        void retryFromDlq_backoffNotDue_nacksWithoutRepublish() throws Exception {
            long deliveryTag = 1L;
            mockQueue(buildGetResponse(deliveryTag, 0, ROUTING_KEY, System.currentTimeMillis()));

            scheduler.retryFromDlq();

            verify(rabbitTemplate, never()).send(eq(RabbitMQConfig.EXCHANGE_NAME), anyString(), any(Message.class));
            verify(channelHolder[0]).basicNack(deliveryTag, false, true);
            var counter = meterRegistry.counter(
                    "easyorange.events.dlq", "queue", ORIGINAL_QUEUE, "reason", "backoff_wait");
            org.junit.jupiter.api.Assertions.assertEquals(1.0, counter.count(), 0.001);
        }

        @Test
        @DisplayName("retryCount=1 未满 5min 的消息不重投")
        void retryFromDlq_backoffNotDue_secondRound_waits() throws Exception {
            mockQueue(buildGetResponse(1L, 1, ROUTING_KEY, System.currentTimeMillis()));

            scheduler.retryFromDlq();

            verify(rabbitTemplate, never()).send(eq(RabbitMQConfig.EXCHANGE_NAME), anyString(), any(Message.class));
            verify(channelHolder[0]).basicNack(1L, false, true);
        }
    }

    @Nested
    @DisplayName("转储 terminal 队列")
    class MoveToTerminalTests {

        @Test
        @DisplayName("retryCount=3 的消息转储 terminal 队列并 ack")
        void retryFromDlq_maxRetries_movesToTerminal() throws Exception {
            long deliveryTag = 1L;
            mockQueue(buildGetResponse(deliveryTag, 3, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), any(Message.class));
            verify(rabbitTemplate, never()).send(eq(RabbitMQConfig.EXCHANGE_NAME), anyString(), any(Message.class));
            verify(channelHolder[0]).basicAck(deliveryTag, false);
        }

        @Test
        @DisplayName("无 x-death 头（无 routing key）的消息转储 terminal")
        void retryFromDlq_noRoutingKey_movesToTerminal() {
            mockQueue(buildGetResponse(1L, 0, null, null));

            scheduler.retryFromDlq();

            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), any(Message.class));
        }

        @Test
        @DisplayName("转储时设置 x-terminal-reason 头")
        void retryFromDlq_maxRetries_setsTerminalReasonHeader() {
            mockQueue(buildGetResponse(1L, 3, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            scheduler.retryFromDlq();

            var msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(eq(RabbitMQConfig.TERMINAL_QUEUE), msgCaptor.capture());
            Object terminalReason = msgCaptor.getValue().getMessageProperties().getHeader("x-terminal-reason");
            assertThat(terminalReason).isEqualTo("max-retries");
        }

        @Test
        @DisplayName("转储后记录 terminal 指标")
        void retryFromDlq_recordsTerminalMetric() {
            mockQueue(buildGetResponse(1L, 3, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            scheduler.retryFromDlq();

            var counter = meterRegistry.counter(
                    "easyorange.events.dlq", "queue", ORIGINAL_QUEUE, "reason", "terminal_max_retries");
            org.junit.jupiter.api.Assertions.assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("空队列处理")
    class EmptyQueueTests {

        @Test
        @DisplayName("DLQ 为空时不发送消息也不 ack/nack")
        void retryFromDlq_emptyQueue_noSend() throws Exception {
            mockEmptyQueue();

            scheduler.retryFromDlq();

            verify(rabbitTemplate, never()).send(anyString(), any(Message.class));
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
            verify(channelHolder[0], never()).basicAck(anyLong(), eq(false));
            verify(channelHolder[0], never()).basicNack(anyLong(), eq(false), eq(true));
        }
    }

    @Nested
    @DisplayName("异常恢复")
    class ExceptionRecoveryTests {

        @Test
        @DisplayName("重投失败时消息 nack 回队，不丢失")
        void retryFromDlq_processingFailure_requeuesMessage() throws Exception {
            long deliveryTag = 1L;
            mockQueue(buildGetResponse(deliveryTag, 0, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000));

            doThrow(new RuntimeException("Connection refused"))
                    .when(rabbitTemplate)
                    .send(eq(RabbitMQConfig.EXCHANGE_NAME), eq(ROUTING_KEY), any(Message.class));

            scheduler.retryFromDlq();

            // 回队由 RabbitMQ 完成：requeue 的 nack 而非重发副本，处理中途崩溃也不丢
            verify(channelHolder[0]).basicNack(deliveryTag, false, true);
            verify(channelHolder[0], never()).basicAck(deliveryTag, false);
        }
    }

    @Nested
    @DisplayName("批处理上限")
    class BatchSizeTests {

        @Test
        @DisplayName("单个 DLQ 队列单次最多处理 10 条消息")
        void retryFromDlq_singleQueueBatchLimit() throws Exception {
            var response = buildGetResponse(1L, 0, ROUTING_KEY, System.currentTimeMillis() - 10 * 60_000);
            // 模拟 basicGet 始终返回消息（不返回 null），BATCH_SIZE 应截断
            when(rabbitTemplate.execute(any(ChannelCallback.class)))
                    .thenAnswer(inv -> {
                        Channel channel = mock(Channel.class);
                        channelHolder[0] = channel;
                        when(channel.basicGet(anyString(), eq(false))).thenReturn(response);
                        return ((ChannelCallback<?>) inv.getArgument(0)).doInRabbit(channel);
                    });

            scheduler.retryFromDlq();

            // 关键断言：即使 basicGet 始终返回消息，单个队列的拉取也被 BATCH_SIZE 截断
            verify(channelHolder[0], times(DlqRetryScheduler.BATCH_SIZE)).basicGet(anyString(), eq(false));
        }
    }
}
