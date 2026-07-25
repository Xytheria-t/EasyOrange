package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem 值对象测试")
class OrderItemTest {

    private static ProductSnapshot snapshot(String name, String price) {
        return new ProductSnapshot("100", name, "img.jpg", "手机",
            Money.of(new BigDecimal(price)), "9成新");
    }

    @Test
    @DisplayName("使用 builder 创建时正确计算 subtotal")
    void create_calculatesSubtotalCorrectly() {
        var snap = snapshot("iPhone", "3999.00");
        OrderItem item = OrderItem.builder()
            .id("1")
            .productId(ProductId.of(snap.productId()))
            .snapshot(snap)
            .unitPrice(snap.price())
            .quantity(2)
            .subtotal(snap.price().multiply(2))
            .build();

        assertThat(item.productId().value()).isEqualTo("100");
        assertThat(item.unitPrice().value()).isEqualByComparingTo(new BigDecimal("3999.00"));
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal().value()).isEqualByComparingTo(new BigDecimal("7998.00"));
        assertThat(item.id()).isEqualTo("1");
    }

    @Test
    @DisplayName("quantity 必须大于 0")
    void create_zeroQuantity_throws() {
        var snap = snapshot("iPhone", "3999.00");
        assertThatThrownBy(() -> OrderItem.builder()
                .id("1")
                .productId(ProductId.of(snap.productId()))
                .snapshot(snap)
                .unitPrice(snap.price())
                .quantity(0)
                .subtotal(snap.price().multiply(0))
                .build())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数量必须大于 0");
    }
}
