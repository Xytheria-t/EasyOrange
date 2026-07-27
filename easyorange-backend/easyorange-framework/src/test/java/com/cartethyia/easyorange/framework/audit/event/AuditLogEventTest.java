package com.cartethyia.easyorange.framework.audit.event;

import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditLogEvent 领域事件")
class AuditLogEventTest {

    private static AuditLog buildAuditLog(String method) {
        return AuditLog.builder()
                .title("商品管理-创建")
                .businessType("1")
                .method(method)
                .requestMethod("POST")
                .requestUrl("/api/products")
                .clientIp("127.0.0.1")
                .username("admin")
                .operatorType(1)
                .status(0)
                .duration(42)
                .build();
    }

    @Nested
    @DisplayName("eventType")
    class EventTypeTests {

        @Test
        @DisplayName("事件类型为 AuditLog（去 Event 后缀）")
        void eventType_returnsAuditLog() {
            var event = AuditLogEvent.of(buildAuditLog("ProductController.create()"));

            assertThat(event.eventType()).isEqualTo("AuditLog");
        }
    }

    @Nested
    @DisplayName("aggregateId")
    class AggregateIdTests {

        @Test
        @DisplayName("聚合标识为审计日志的方法签名")
        void aggregateId_returnsMethod() {
            var auditLog = buildAuditLog("ProductController.create()");
            var event = AuditLogEvent.of(auditLog);

            assertThat(event.aggregateId()).isEqualTo("ProductController.create()");
        }

        @Test
        @DisplayName("method 为 null 时聚合标识返回 unknown")
        void aggregateId_whenMethodIsNull_returnsUnknown() {
            var auditLog = AuditLog.builder()
                    .title("test")
                    .method(null)
                    .build();
            var event = AuditLogEvent.of(auditLog);

            assertThat(event.aggregateId()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("of 工厂方法")
    class OfFactoryTests {

        @Test
        @DisplayName("of 方法包装 AuditLog 实体")
        void of_wrapsAuditLog() {
            var auditLog = buildAuditLog("OrderController.create()");

            var event = AuditLogEvent.of(auditLog);

            assertThat(event.auditLog()).isSameAs(auditLog);
        }
    }
}
