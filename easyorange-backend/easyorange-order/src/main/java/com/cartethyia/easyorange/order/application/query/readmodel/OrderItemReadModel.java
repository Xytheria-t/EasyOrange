package com.cartethyia.easyorange.order.application.query.readmodel;

import java.math.BigDecimal;

public record OrderItemReadModel(
        String itemId,
        String productId,
        String productSnapshot,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal) {}
