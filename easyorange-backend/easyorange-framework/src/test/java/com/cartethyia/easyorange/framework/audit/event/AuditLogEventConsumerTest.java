package com.cartethyia.easyorange.framework.audit.event;

import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogEventConsumer 单元测试")
class AuditLogEventConsumerTest {

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private AuditLogService auditLogService;

    private AuditLogEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new AuditLogEventConsumer(idempotencyChecker, metricsService, auditLogService);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    private AuditLog buildAuditLog() {
        return AuditLog.builder()
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
                .build();
    }

    @Nested
    @DisplayName("正常消费")
    class NormalConsumptionTests {

        @Test
        @DisplayName("接收 AuditLogEvent 后调用 insertAuditLog 入库")
        void handle_auditLogEvent_callsInsertAuditLog() {
            var auditLog = buildAuditLog();
            var event = AuditLogEvent.of(auditLog);

            consumer.onAuditLog(event, buildMessage());

            var captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogService).insertAuditLog(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getMethod()).isEqualTo("ProductController.create()");
            assertThat(saved.getTitle()).isEqualTo("商品管理-创建");
            assertThat(saved.getStatus()).isEqualTo(0);
            assertThat(saved.getDuration()).isEqualTo(55);
        }

        @Test
        @DisplayName("审计日志消费者关闭幂等检查（idempotencyEnabled=false）")
        void handle_doesNotCheckIdempotency() {
            var event = AuditLogEvent.of(buildAuditLog());

            consumer.onAuditLog(event, buildMessage());

            verify(idempotencyChecker, never()).isDuplicate(any(), any());
            verify(idempotencyChecker, never()).tryMark(any(), any());
        }

        @Test
        @DisplayName("异常状态审计日志也能正常入库")
        void handle_errorStatusAuditLog_persistsCorrectly() {
            var auditLog = AuditLog.builder()
                    .title("订单管理-删除")
                    .businessType("3")
                    .method("OrderController.delete()")
                    .requestMethod("DELETE")
                    .status(1)
                    .errorMsg("订单不存在")
                    .duration(12)
                    .build();
            var event = AuditLogEvent.of(auditLog);

            consumer.onAuditLog(event, buildMessage());

            var captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogService).insertAuditLog(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(1);
            assertThat(captor.getValue().getErrorMsg()).isEqualTo("订单不存在");
        }
    }
}
