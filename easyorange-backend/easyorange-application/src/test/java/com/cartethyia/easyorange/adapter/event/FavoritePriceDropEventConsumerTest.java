package com.cartethyia.easyorange.adapter.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoritePriceDropEventConsumer 单元测试")
class FavoritePriceDropEventConsumerTest {

    private static final String CONSUMER_ID = "FavoritePriceDropEventConsumer";

    @Mock
    private EventIdempotencyChecker idempotencyChecker;

    @Mock
    private FavoriteService favoriteService;

    private FavoritePriceDropEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var metricsService = new EventMetricsService(new SimpleMeterRegistry());
        consumer = new FavoritePriceDropEventConsumer(idempotencyChecker, metricsService, favoriteService);
    }

    private Message buildMessage() {
        var props = new MessageProperties();
        props.setMessageId(UUID.randomUUID().toString());
        return new Message(new byte[0], props);
    }

    private ProductUpdatedEvent buildEvent(BigDecimal price) {
        ProductEvent.Data data = new ProductEvent.Data(
                "prod-1",
                "seller-1",
                "cat-1",
                "测试商品",
                price,
                new BigDecimal("120.00"),
                5,
                "USED",
                "北京",
                "微信",
                "描述",
                List.of("url"));
        return new ProductUpdatedEvent("evt-1", data);
    }

    @Test
    @DisplayName("商品更新事件触发降价处理（载荷自包含商品名与价格）")
    void onProductUpdated_delegatesToService() {
        when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(true);

        consumer.onProductUpdated(buildEvent(new BigDecimal("80.00")), buildMessage());

        verify(favoriteService).processPriceDrop("prod-1", "测试商品", new BigDecimal("80.00"));
        verify(idempotencyChecker).tryMark(anyString(), anyString());
    }

    @Test
    @DisplayName("重复事件被幂等检查拦截时跳过处理")
    void onProductUpdated_duplicateEvent_skips() {
        when(idempotencyChecker.tryMark(anyString(), anyString())).thenReturn(false);

        consumer.onProductUpdated(buildEvent(new BigDecimal("80.00")), buildMessage());

        verify(favoriteService, never()).processPriceDrop(any(), any(), any());
    }
}
