package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import com.cartethyia.easyorange.product.domain.event.OfferAcceptedEvent;
import com.cartethyia.easyorange.product.domain.event.OfferCounteredEvent;
import com.cartethyia.easyorange.product.domain.event.OfferRejectedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfferEventConsumer 单元测试")
class OfferEventConsumerTest {

    @Mock
    private WebSocketNotifier webSocketNotifier;

    @InjectMocks
    private OfferEventConsumer consumer;

    private static final Long PRODUCT_ID = 100L;
    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 1L;
    private static final Long ORDER_ID = 500L;
    private static final BigDecimal ACCEPTED_PRICE = new BigDecimal("150.00");
    private static final BigDecimal OFFER_PRICE = new BigDecimal("100.00");
    private static final BigDecimal COUNTER_PRICE = new BigDecimal("130.00");

    @Nested
    @DisplayName("onOfferAccepted")
    class OnOfferAcceptedTests {

        @Test
        @DisplayName("接收出价接受事件后调用 notifyOfferAccepted")
        void onOfferAccepted_shouldNotifyBuyer() {
            OfferAcceptedEvent event = new OfferAcceptedEvent(
                    PRODUCT_ID, SELLER_ID, BUYER_ID, ACCEPTED_PRICE, ORDER_ID);

            consumer.onOfferAccepted(event);

            verify(webSocketNotifier).notifyOfferAccepted(BUYER_ID, PRODUCT_ID, ACCEPTED_PRICE);
        }
    }

    @Nested
    @DisplayName("onOfferRejected")
    class OnOfferRejectedTests {

        @Test
        @DisplayName("接收出价拒绝事件后调用 notifyOfferRejected")
        void onOfferRejected_shouldNotifyBuyer() {
            OfferRejectedEvent event = new OfferRejectedEvent(
                    PRODUCT_ID, SELLER_ID, BUYER_ID, OFFER_PRICE);

            consumer.onOfferRejected(event);

            verify(webSocketNotifier).notifyOfferRejected(BUYER_ID, PRODUCT_ID, OFFER_PRICE);
        }
    }

    @Nested
    @DisplayName("onOfferCountered")
    class OnOfferCounteredTests {

        @Test
        @DisplayName("接收还价事件后调用 notifyCounterOffer")
        void onOfferCountered_shouldNotifyBuyer() {
            OfferCounteredEvent event = new OfferCounteredEvent(
                    PRODUCT_ID, SELLER_ID, BUYER_ID, OFFER_PRICE, COUNTER_PRICE);

            consumer.onOfferCountered(event);

            verify(webSocketNotifier).notifyCounterOffer(BUYER_ID, PRODUCT_ID, COUNTER_PRICE);
        }
    }
}
