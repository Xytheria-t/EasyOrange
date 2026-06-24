package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.port.NegotiationContext;
import com.cartethyia.easyorange.product.domain.port.NegotiationMessagePort;
import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.OfferAcceptedEvent;
import com.cartethyia.easyorange.product.domain.event.OfferCounteredEvent;
import com.cartethyia.easyorange.product.domain.event.OfferRejectedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.OrderCreationPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.service.OfferRuleEngine;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("议价应用服务测试")
class OfferAppServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OfferRuleEngine offerRuleEngine;
    @Mock
    private NegotiationMessagePort negotiationMessagePort;
    @Mock
    private OrderCreationPort orderCreationPort;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private OfferAppService offerAppService;

    private Product aiManagedOnlineProduct;
    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final BigDecimal FLOOR_PRICE = new BigDecimal("50.00");
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("100.00");

    @BeforeEach
    void setUp() {
        offerAppService = new OfferAppService(
                productRepository, offerRuleEngine, negotiationMessagePort,
                orderCreationPort, domainEventPublisher);

        // 创建一个 AI 托管且上架中的商品
        ProductCreatedResult createResult = Product.create(
                SellerId.of(SELLER_ID),
                CategoryId.of(2L),
                ProductTitle.of("AI托管商品测试"),
                Money.of(CURRENT_PRICE),
                null,
                Money.of(FLOOR_PRICE),
                ConsignmentMode.AI_MANAGED,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(java.util.List.of("http://img/1.jpg"))
        );
        aiManagedOnlineProduct = createResult.product().assignId(PRODUCT_ID);
        // 上架
        aiManagedOnlineProduct = aiManagedOnlineProduct.putOnline();
    }

    // ==================== 出价 >= 底价 → ACCEPT ====================

    @Test
    @DisplayName("出价 >= 底价 → ACCEPT，创建订单，返回 orderId")
    void processOffer_offerAtOrAboveFloorPrice_shouldAccept() {
        BigDecimal offerPrice = new BigDecimal("50.00");
        Long expectedOrderId = 999L;

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(aiManagedOnlineProduct));
        when(offerRuleEngine.evaluate(any(), any(), any(), anyInt()))
                .thenReturn(new OfferDecision(
                        OfferDecision.DecisionType.ACCEPT,
                        offerPrice, null, "出价达到底价，直接成交"));
        when(negotiationMessagePort.generateMessage(any(NegotiationContext.class)))
                .thenReturn("好的，¥50成交！请尽快付款哦~");
        when(orderCreationPort.createOrder(BUYER_ID, PRODUCT_ID, offerPrice))
                .thenReturn(expectedOrderId);

        OfferResult result = offerAppService.processOffer(BUYER_ID, PRODUCT_ID, offerPrice);

        assertThat(result.decisionType()).isEqualTo("ACCEPT");
        assertThat(result.message()).isNotBlank();
        assertThat(result.orderId()).isEqualTo(expectedOrderId);
        assertThat(result.counterPrice()).isNull();

        verify(orderCreationPort).createOrder(BUYER_ID, PRODUCT_ID, offerPrice);
        verify(domainEventPublisher).publish(any(OfferAcceptedEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferRejectedEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferCounteredEvent.class));
    }

    // ==================== 出价 >= 底价 × 0.9 → COUNTER ====================

    @Test
    @DisplayName("出价 >= 底价 × 0.9 → COUNTER，返回 counterPrice，不创建订单")
    void processOffer_offerAtCounterThreshold_shouldCounter() {
        BigDecimal offerPrice = new BigDecimal("45.00");  // 底价50 × 0.9 = 45
        BigDecimal counterPrice = new BigDecimal("47.50"); // 底价50 × 0.95 = 47.5

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(aiManagedOnlineProduct));
        when(offerRuleEngine.evaluate(any(), any(), any(), anyInt()))
                .thenReturn(new OfferDecision(
                        OfferDecision.DecisionType.COUNTER,
                        null, counterPrice, "出价接近底价，还价至底价95%"));
        when(negotiationMessagePort.generateMessage(any(NegotiationContext.class)))
                .thenReturn("¥45有点低啦，¥47.5可以吗？");

        OfferResult result = offerAppService.processOffer(BUYER_ID, PRODUCT_ID, offerPrice);

        assertThat(result.decisionType()).isEqualTo("COUNTER");
        assertThat(result.counterPrice()).isEqualByComparingTo(counterPrice);
        assertThat(result.orderId()).isNull();

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher).publish(any(OfferCounteredEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferAcceptedEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferRejectedEvent.class));
    }

    // ==================== 出价 < 底价 × 0.9 → REJECT ====================

    @Test
    @DisplayName("出价 < 底价 × 0.9 → REJECT，不创建订单，不返回 counterPrice")
    void processOffer_offerBelowThreshold_shouldReject() {
        BigDecimal offerPrice = new BigDecimal("40.00");  // 低于底价50 × 0.9 = 45

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(aiManagedOnlineProduct));
        when(offerRuleEngine.evaluate(any(), any(), any(), anyInt()))
                .thenReturn(new OfferDecision(
                        OfferDecision.DecisionType.REJECT,
                        null, null, "出价过低"));
        when(negotiationMessagePort.generateMessage(any(NegotiationContext.class)))
                .thenReturn("抱歉，¥40太低啦，再考虑考虑？");

        OfferResult result = offerAppService.processOffer(BUYER_ID, PRODUCT_ID, offerPrice);

        assertThat(result.decisionType()).isEqualTo("REJECT");
        assertThat(result.counterPrice()).isNull();
        assertThat(result.orderId()).isNull();

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher).publish(any(OfferRejectedEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferAcceptedEvent.class));
        verify(domainEventPublisher, never()).publish(any(OfferCounteredEvent.class));
    }

    // ==================== 商品非 AI_MANAGED → 异常 ====================

    @Test
    @DisplayName("非 AI 托管商品 → 抛出 IllegalStateException")
    void processOffer_notAiManaged_shouldThrow() {
        ProductCreatedResult createResult = Product.create(
                SellerId.of(SELLER_ID),
                CategoryId.of(2L),
                ProductTitle.of("手动商品"),
                Money.of(CURRENT_PRICE),
                null,
                null,
                ConsignmentMode.MANUAL,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(java.util.List.of("http://img/1.jpg"))
        );
        Product manualProduct = createResult.product().assignId(PRODUCT_ID).putOnline();

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(manualProduct));

        assertThatThrownBy(() -> offerAppService.processOffer(BUYER_ID, PRODUCT_ID, new BigDecimal("50")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不支持AI议价");

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher, never()).publish(any(BaseDomainEvent.class));
    }

    // ==================== 商品已下架 → 异常 ====================

    @Test
    @DisplayName("商品已下架 → 抛出 IllegalStateException")
    void processOffer_offlineProduct_shouldThrow() {
        Product offlineProduct = aiManagedOnlineProduct.takeOffline();

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(offlineProduct));

        assertThatThrownBy(() -> offerAppService.processOffer(BUYER_ID, PRODUCT_ID, new BigDecimal("50")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已下架");

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher, never()).publish(any(BaseDomainEvent.class));
    }

    // ==================== 买自己的商品 → 异常 ====================

    @Test
    @DisplayName("买自己的商品 → 抛出 IllegalStateException")
    void processOffer_buyOwnProduct_shouldThrow() {
        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(aiManagedOnlineProduct));

        // 用同一个 SELLER_ID 作为 buyerId
        assertThatThrownBy(() -> offerAppService.processOffer(SELLER_ID, PRODUCT_ID, new BigDecimal("50")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不能购买自己的商品");

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher, never()).publish(any(BaseDomainEvent.class));
    }

    // ==================== 商品不存在 → 异常 ====================

    @Test
    @DisplayName("商品不存在 → 抛出 ProductNotFoundException")
    void processOffer_productNotFound_shouldThrow() {
        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerAppService.processOffer(BUYER_ID, PRODUCT_ID, new BigDecimal("50")))
                .isInstanceOf(ProductNotFoundException.class);

        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher, never()).publish(any(BaseDomainEvent.class));
    }

    // ==================== LLM 话术生成失败 → 使用兜底话术 ====================

    @Test
    @DisplayName("LLM 话术生成失败应抛异常，事务回滚")
    void processOffer_llmFails_shouldThrowAndRollback() {
        BigDecimal offerPrice = new BigDecimal("60.00");

        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(aiManagedOnlineProduct));
        when(offerRuleEngine.evaluate(any(), any(), any(), anyInt()))
                .thenReturn(new OfferDecision(
                        OfferDecision.DecisionType.ACCEPT,
                        offerPrice, null, "出价达到底价，直接成交"));
        // LLM 抛异常
        when(negotiationMessagePort.generateMessage(any(NegotiationContext.class)))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        // 异常会传播出去（而非被吞掉），因为兜底逻辑在 adapter 层，不在 service 层
        assertThatThrownBy(() -> offerAppService.processOffer(BUYER_ID, PRODUCT_ID, offerPrice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM service unavailable");

        // 事务回滚，不应创建订单
        verify(orderCreationPort, never()).createOrder(any(), any(), any());
        verify(domainEventPublisher, never()).publish(any(BaseDomainEvent.class));
    }
}
