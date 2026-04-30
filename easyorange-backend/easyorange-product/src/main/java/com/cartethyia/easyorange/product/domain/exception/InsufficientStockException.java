package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message, ProductId productId, StockQuantity currentStock) {
        super(message + " (productId=" + (productId != null ? productId.value() : "null")
                + ", stock=" + (currentStock != null ? currentStock.value() : "null") + ")");
    }
}
