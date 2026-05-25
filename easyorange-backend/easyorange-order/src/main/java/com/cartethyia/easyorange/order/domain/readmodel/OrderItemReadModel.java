package com.cartethyia.easyorange.order.domain.readmodel;

import java.math.BigDecimal;

public record OrderItemReadModel(
    Long itemId,
    Long productId,
    String productSnapshot,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal
) {}
