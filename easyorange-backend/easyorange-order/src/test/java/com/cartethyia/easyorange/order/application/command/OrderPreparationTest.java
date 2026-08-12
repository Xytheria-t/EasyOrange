package com.cartethyia.easyorange.order.application.command;

import static com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderPreparation 订单项准备测试")
class OrderPreparationTest {

    @Mock
    private ProductOrderPort productOrderPort;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private IdGenerator idGenerator;

    private OrderPreparation preparation;

    private static final String SELLER_ID = "2";
    private static final String ITEM_ID = "018f7c1d-0000-7000-8000-000000000001";

    @BeforeEach
    void setUp() {
        preparation = new OrderPreparation(productOrderPort, productQueryPort, idGenerator);
        when(idGenerator.generateId()).thenReturn(ITEM_ID);
    }

    private static CreateOrderItem item(String productId, int quantity) {
        return new CreateOrderItem(productId, quantity);
    }

    private static ProductSnapshot snapshot(String productId, String sellerId, boolean online, int stock) {
        return new ProductSnapshot(productId, sellerId, new BigDecimal("99.99"), online, stock);
    }

    private static ProductDetail detail(String productId) {
        return new ProductDetail(
                productId, "资产-" + productId, new BigDecimal("99.99"), "ONLINE", List.of("img1"), "描述", "A");
    }

    @Test
    @DisplayName("成功准备：返回资产方 ID 与回填详情的订单项")
    void prepare_validItems_returnsSellerAndEnrichedOrderItems() {
        when(productOrderPort.getSnapshots(any()))
                .thenReturn(List.of(snapshot("100", SELLER_ID, true, 10), snapshot("101", SELLER_ID, true, 3)));
        when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(detail("100"), detail("101")));

        var result = preparation.prepareOrderItems(List.of(item("100", 2), item("101", 1)));

        assertThat(result.sellerId().value()).isEqualTo(SELLER_ID);
        assertThat(result.orderItems()).hasSize(2);

        var first = result.orderItems().getFirst();
        assertThat(first.id()).isEqualTo(ITEM_ID);
        assertThat(first.productId().value()).isEqualTo("100");
        assertThat(first.snapshot().name()).isEqualTo("资产-100");
        assertThat(first.snapshot().image()).isEqualTo("img1");
        assertThat(first.snapshot().conditionLevel()).isEqualTo("A");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.subtotal().value()).isEqualByComparingTo(new BigDecimal("199.98"));
    }

    @Test
    @DisplayName("资产不存在时抛异常")
    void prepare_missingProduct_throws() {
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of());

        assertThatThrownBy(() -> preparation.prepareOrderItems(List.of(item("999", 1))))
                .isInstanceOf(OrderDomainException.class)
                .hasMessageContaining("资产不存在: 999");
    }

    @Test
    @DisplayName("资产已下架时抛异常")
    void prepare_offlineProduct_throws() {
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of(snapshot("100", SELLER_ID, false, 10)));

        assertThatThrownBy(() -> preparation.prepareOrderItems(List.of(item("100", 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资产已下架");
    }

    @Test
    @DisplayName("资产库存不足时抛异常")
    void prepare_noStock_throws() {
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of(snapshot("100", SELLER_ID, true, 0)));

        assertThatThrownBy(() -> preparation.prepareOrderItems(List.of(item("100", 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资产库存不足");
    }

    @Test
    @DisplayName("订单项来自不同资产方时抛异常")
    void prepare_differentSellers_throws() {
        when(productOrderPort.getSnapshots(any()))
                .thenReturn(List.of(snapshot("100", SELLER_ID, true, 10), snapshot("101", "3", true, 10)));

        assertThatThrownBy(() -> preparation.prepareOrderItems(List.of(item("100", 1), item("101", 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须来自同一资产方");
    }

    @Test
    @DisplayName("快照存在但详情缺失时抛异常（跨读源不一致，回滚而非写入脏快照）")
    void prepare_missingDetail_throws() {
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of(snapshot("100", SELLER_ID, true, 10)));
        // getProductsByIds 未打桩 → 返回空列表，模拟详情读源缺数据

        assertThatThrownBy(() -> preparation.prepareOrderItems(List.of(item("100", 1))))
                .isInstanceOf(OrderDomainException.class)
                .hasMessageContaining("资产详情缺失: 100");
    }
}
