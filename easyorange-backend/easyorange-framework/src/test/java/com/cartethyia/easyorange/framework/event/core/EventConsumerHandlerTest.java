package com.cartethyia.easyorange.framework.event.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.framework.audit.event.AuditLogEvent;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * EventConsumerHandler 单元测试 — 「原子领取处理权 → 处理 → 失败撤销标记」编排语义。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumerHandler 事件消费编排")
class EventConsumerHandlerTest {

    private static final String CONSUMER_ID = "TestConsumer";
    private static final String NAMESPACE = CONSUMER_ID + ":AuditLog";
    private static final String EVENT_ID = "0196a1c2-0000-7000-8000-000000000001";

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    private EventConsumerHandler handler;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        handler = new EventConsumerHandler(CONSUMER_ID, idempotencyChecker, metricsService);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(EVENT_ID);
        return new Message(new byte[0], props);
    }

    private AuditLogEvent buildEvent() {
        // 事件自带固定 eventId（生产代码由 UuidV7 生成，此处注入常量以便断言幂等键）
        return new AuditLogEvent(
                EVENT_ID,
                AuditLog.builder()
                        .title("商品管理-创建")
                        .businessType("1")
                        .method("ProductController.create()")
                        .requestMethod("POST")
                        .requestUrl("/api/products")
                        .clientIp("192.168.1.100")
                        .username("admin")
                        .operatorType(1)
                        .status(0)
                        .duration(55)
                        .requestParams("{\"name\":\"手机\"}")
                        .build());
    }

    @Nested
    @DisplayName("幂等领取")
    class ClaimTests {

        @Test
        @DisplayName("领取处理权成功 → 执行消费者逻辑")
        void handle_whenClaimed_executesConsumer() {
            when(idempotencyChecker.tryMark(eq(NAMESPACE), eq(EVENT_ID))).thenReturn(true);

            AtomicBoolean executed = new AtomicBoolean(false);
            handler.handle(buildEvent(), buildMessage(), () -> executed.set(true));

            assertThat(executed).isTrue();
        }

        @Test
        @DisplayName("重复事件（领取失败）→ 跳过，不执行消费者逻辑")
        void handle_whenDuplicate_skipsConsumer() {
            when(idempotencyChecker.tryMark(eq(NAMESPACE), eq(EVENT_ID))).thenReturn(false);

            AtomicBoolean executed = new AtomicBoolean(false);
            handler.handle(buildEvent(), buildMessage(), () -> executed.set(true));

            assertThat(executed).isFalse();
        }

        @Test
        @DisplayName("事件无 eventId（历史消息载荷缺失）→ 跳过幂等检查，正常处理")
        void handle_withoutEventId_skipsIdempotencyCheck() {
            var message = new Message(new byte[0], new MessageProperties());
            var eventWithoutId = new AuditLogEvent(
                    null,
                    AuditLog.builder()
                            .title("商品管理-创建")
                            .businessType("1")
                            .method("ProductController.create()")
                            .requestMethod("POST")
                            .requestUrl("/api/products")
                            .clientIp("192.168.1.100")
                            .username("admin")
                            .operatorType(1)
                            .status(0)
                            .duration(55)
                            .requestParams("{\"name\":\"手机\"}")
                            .build());

            AtomicBoolean executed = new AtomicBoolean(false);
            handler.handle(eventWithoutId, message, () -> executed.set(true));

            assertThat(executed).isTrue();
            verify(idempotencyChecker, never()).tryMark(any(), any());
        }
    }

    @Nested
    @DisplayName("失败恢复")
    class FailureTests {

        @Test
        @DisplayName("处理抛异常 → 撤销标记并重抛原异常")
        void handle_whenConsumerFails_unmarksAndRethrows() {
            when(idempotencyChecker.tryMark(eq(NAMESPACE), eq(EVENT_ID))).thenReturn(true);
            var failure = new IllegalStateException("boom");

            assertThatThrownBy(() -> handler.handle(buildEvent(), buildMessage(), () -> {
                        throw failure;
                    }))
                    .isSameAs(failure);

            verify(idempotencyChecker).unmark(eq(NAMESPACE), eq(EVENT_ID));
        }

        @Test
        @DisplayName("幂等关闭 → 不领取也不撤销标记")
        void handle_whenIdempotencyDisabled_neverTouchesChecker() {
            var metricsService = new EventMetricsService(new SimpleMeterRegistry());
            handler = new EventConsumerHandler(CONSUMER_ID, idempotencyChecker, metricsService, false);

            AtomicBoolean executed = new AtomicBoolean(false);
            handler.handle(buildEvent(), buildMessage(), () -> executed.set(true));

            assertThat(executed).isTrue();
            verify(idempotencyChecker, never()).tryMark(any(), any());
            verify(idempotencyChecker, never()).unmark(any(), any());
        }
    }
}
