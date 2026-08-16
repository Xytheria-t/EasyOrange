package com.cartethyia.easyorange.product.adapter.inbound.messaging;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private ProductCacheEvictionPort cacheEvictionPort;

    @Mock
    private ProductQueryHandler productQueryHandler;

    @Mock
    private ProductNotificationPort notificationPort;

    @Mock
    private ProductSearchIndexPort searchIndexPort;

    private ProductEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ProductEventConsumer(
                mock(EventIdempotencyChecker.class),
                new EventMetricsService(new SimpleMeterRegistry()),
                cacheEvictionPort,
                productQueryHandler,
                notificationPort,
                searchIndexPort);
    }

    @Test
    @DisplayName("审核事件应失效缓存并同步 ES 索引状态")
    void auditedEvent_shouldEvictCacheAndUpdateIndex() {
        var event = new ProductAuditedEvent(
                "e-1", "p-1", "手机", "seller-1", AuditAction.APPROVED, "通过", LocalDateTime.now());

        consumer.handle(event, new Message(new byte[0], new MessageProperties()));

        verify(cacheEvictionPort).evictProductCache("p-1");
        verify(searchIndexPort).updateProductIndex("p-1");
    }

    @Test
    @DisplayName("创建事件应建索引并发送通知")
    void createdEvent_shouldIndexAndNotify() {
        var event = new ProductCreatedEvent("e-2", productData());

        consumer.handle(event, new Message(new byte[0], new MessageProperties()));

        verify(notificationPort).notifyProductCreated("p-1", "u-1");
        verify(searchIndexPort).indexProduct("p-1");
    }

    @Test
    @DisplayName("举报处理事件路由到 report.# 队列，本队列应无副作用")
    void reportProcessedEvent_shouldBeIgnored() {
        var event = new ReportProcessedEvent("e-3", "r-1", "reporter-1", "p-1", true, "违规", LocalDateTime.now());

        consumer.handle(event, new Message(new byte[0], new MessageProperties()));

        verify(cacheEvictionPort, never()).evictProductCache(anyString());
        verify(searchIndexPort, never()).updateProductIndex(anyString());
    }

    private static ProductEvent.Data productData() {
        return new ProductEvent.Data(
                "p-1",
                "u-1",
                "c-1",
                "手机",
                BigDecimal.valueOf(999.00),
                null,
                1,
                "NEW",
                "北京",
                "wechat",
                "九成新手机",
                List.of("https://example.com/1.jpg"));
    }
}
