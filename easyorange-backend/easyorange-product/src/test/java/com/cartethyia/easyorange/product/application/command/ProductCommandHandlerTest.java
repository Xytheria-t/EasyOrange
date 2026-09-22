package com.cartethyia.easyorange.product.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductTestFixture;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import com.cartethyia.easyorange.product.domain.valueobject.StockChange;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品命令处理器测试")
class ProductCommandHandlerTest {

    private static final String SELLER_ID = "1";
    private static final String PRODUCT_ID = "1";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockLedgerRepository stockLedgerRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private BusinessMetricsService businessMetricsService;

    private ProductCommandHandler commandHandler;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        commandHandler = new ProductCommandHandler(
                productRepository, stockLedgerRepository, domainEventPublisher, businessMetricsService);
        existingProduct = ProductTestFixture.onlineProduct();
    }

    @Test
    @DisplayName("创建商品应调用仓储保存并落库存基线")
    void createProduct_shouldSaveToRepository() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return p.assignId("42");
        });

        CreateProductCommand command = new CreateProductCommand(
                "2",
                "测试商品",
                new BigDecimal("100"),
                null,
                10,
                "1",
                "北京",
                "微信",
                "描述",
                java.util.List.of("http://img/1.jpg"),
                null);

        String productId = commandHandler.createProduct(SELLER_ID, command);

        assertThat(productId).isEqualTo("42");
        verify(productRepository).save(any(Product.class));
        var eventCaptor = org.mockito.ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        // 发布事件必须携带落库后的聚合 ID，否则下游 ES 索引与通知拿不到 productId
        assertThat(eventCaptor.getValue().productId()).isEqualTo("42");
        verify(stockLedgerRepository)
                .record(argThat(change -> change.changeType() == StockChangeType.INIT
                        && change.bizId() == null
                        && change.delta() == 10
                        && change.stockAfter() == 10));
    }

    @Test
    @DisplayName("库存为 0 的资产也能创建（基线流水 delta=0，下单接口不限制 stock=0）")
    void createProduct_zeroStock_recordsZeroBaseline() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> ((Product) invocation.getArgument(0)).assignId("43"));

        CreateProductCommand command = new CreateProductCommand(
                "2",
                "零库存资产",
                new BigDecimal("100"),
                null,
                0,
                "1",
                "北京",
                "微信",
                "描述",
                java.util.List.of("http://img/1.jpg"),
                null);

        assertThat(commandHandler.createProduct(SELLER_ID, command)).isEqualTo("43");
        verify(stockLedgerRepository).record(argThat(change -> change.delta() == 0 && change.stockAfter() == 0));
    }

    @Test
    @DisplayName("更新不存在的商品应抛出异常")
    void updateProduct_whenNotFound_shouldThrow() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

        UpdateProductCommand command =
                new UpdateProductCommand("999", null, "新名称", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> commandHandler.updateProduct(SELLER_ID, command))
                .isInstanceOf(ProductDomainException.class);
    }

    @Test
    @DisplayName("卖家改库存应补落人工调整流水，避免对账误判为漂移")
    void updateProduct_whenStockChanged_shouldRecordAdjustment() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

        UpdateProductCommand command =
                new UpdateProductCommand(PRODUCT_ID, null, null, null, null, 4, null, null, null, null, null);

        commandHandler.updateProduct(SELLER_ID, command);

        verify(stockLedgerRepository)
                .record(argThat(change -> change.changeType() == StockChangeType.ADJUST
                        && change.delta() == -6
                        && change.stockAfter() == 4));
    }

    @Test
    @DisplayName("未改库存的普通编辑不应产生流水")
    void updateProduct_whenStockUnchanged_shouldNotRecord() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

        UpdateProductCommand command =
                new UpdateProductCommand(PRODUCT_ID, null, "新名称", null, null, null, null, null, null, null, null);

        commandHandler.updateProduct(SELLER_ID, command);

        verify(stockLedgerRepository, never()).record(any());
    }

    @Test
    @DisplayName("首次扣减库存应落账后保存订单侧变更并发布事件")
    void decrementStock_shouldRecordLedgerThenSave() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(stockLedgerRepository.recordIfAbsent(any())).thenReturn(true);

        commandHandler.decrementStock("order-1", PRODUCT_ID, 2);

        verify(stockLedgerRepository)
                .recordIfAbsent(argThat(change -> change.changeType() == StockChangeType.DECREASE
                        && "order-1".equals(change.bizId())
                        && change.delta() == -2
                        && change.stockAfter() == 8));
        verify(productRepository).save(any(Product.class));
        verify(domainEventPublisher).publish(any(StockDecreasedEvent.class));
    }

    @Test
    @DisplayName("流水已落账的扣减（重复投递）应整体跳过：不改库存也不发事件")
    void decrementStock_whenDuplicate_shouldSkip() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(stockLedgerRepository.recordIfAbsent(any())).thenReturn(false);

        commandHandler.decrementStock("order-1", PRODUCT_ID, 2);

        verify(productRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
        verify(businessMetricsService).incrementStockChangeSkipped();
    }

    @Test
    @DisplayName("恢复库存应按订单下单的数量回补，而非固定 1")
    void restoreStock_shouldRestoreOrderQuantity() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(stockLedgerRepository.recordIfAbsent(any())).thenReturn(true);

        commandHandler.restoreStock("order-1", PRODUCT_ID, 3);

        verify(stockLedgerRepository)
                .recordIfAbsent(argThat(change -> change.changeType() == StockChangeType.RESTORE
                        && change.delta() == 3
                        && change.stockAfter() == 13));
        verify(domainEventPublisher).publish(any(StockRestoredEvent.class));
    }

    @Test
    @DisplayName("恢复库存的流水已落账时应跳过，库存不被二次加回")
    void restoreStock_whenDuplicate_shouldSkip() {
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(stockLedgerRepository.recordIfAbsent(any())).thenReturn(false);

        commandHandler.restoreStock("order-1", PRODUCT_ID, 3);

        verify(productRepository, never()).save(any());
        verify(businessMetricsService).incrementStockChangeSkipped();
    }

    @Test
    @DisplayName("库存流水拒绝无业务单号的扣减/恢复变更")
    void stockChange_shouldRequireBizIdForOrderDrivenTypes() {
        var productId = ProductId.of(PRODUCT_ID);

        assertThatThrownBy(() -> StockChange.decrease(null, productId, 1, 9)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> StockChange.restore(null, productId, 1, 9)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("库存流水拒绝零数量的扣减/恢复，但允许库存为 0 的资产建基线（下单接口不限制 stock=0）")
    void stockChange_shouldRejectZeroQuantity() {
        var productId = ProductId.of(PRODUCT_ID);

        assertThatThrownBy(() -> StockChange.decrease("order-1", productId, 0, 9))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> StockChange.restore("order-1", productId, 0, 9))
                .isInstanceOf(RuntimeException.class);
        assertThat(new StockChange(StockChangeType.INIT, null, productId, 0, 0).delta())
                .isZero();
    }
}
