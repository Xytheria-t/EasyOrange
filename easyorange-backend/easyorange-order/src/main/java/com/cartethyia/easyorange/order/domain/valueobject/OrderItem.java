package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItem(
    Long id,
    ProductId productId,
    ProductSnapshot snapshot,
    Money unitPrice,
    int quantity,
    Money subtotal
) {
    public OrderItem {
        BizRequire.requireTrue(quantity > 0, "数量必须大于 0");
        BizRequire.requireTrue(subtotal.amount().compareTo(BigDecimal.ZERO) > 0, "小计金额必须大于 0");
    }

    public static OrderItem create(ProductSnapshot snapshot, int quantity) {
        Money unitPrice = snapshot.price();
        return OrderItem.builder()
            .id(SnowflakeIdGenerator.getInstance().nextId())
            .productId(ProductId.of(snapshot.productId()))
            .snapshot(snapshot)
            .unitPrice(unitPrice)
            .quantity(quantity)
            .subtotal(unitPrice.multiply(quantity))
            .build();
    }
}
