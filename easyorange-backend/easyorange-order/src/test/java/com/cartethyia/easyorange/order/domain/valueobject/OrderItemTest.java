package com.cartethyia.easyorange.order.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem 值对象测试")
class OrderItemTest {

    @Test
    @DisplayName("创建行项时正确计算 subtotal")
    void create_calculatesSubtotalCorrectly() {
        var snapshot = new ProductSnapshot(100L, "iPhone", "img.jpg", "手机",
            Money.of(new BigDecimal("3999.00")), "9成新");
        OrderItem item = OrderItem.create(snapshot, 2);

        assertThat(item.productId().value()).isEqualTo(100L);
        assertThat(item.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("3999.00"));
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal().amount()).isEqualByComparingTo(new BigDecimal("7998.00"));
        assertThat(item.id()).isNotNull();
    }

    @Test
    @DisplayName("quantity 必须大于 0")
    void create_zeroQuantity_throws() {
        var snapshot = new ProductSnapshot(100L, "iPhone", "img.jpg", "手机",
            Money.of(new BigDecimal("3999.00")), "9成新");
        assertThatThrownBy(() -> OrderItem.create(snapshot, 0))
                .isInstanceOf(Exception.class);
    }
}
