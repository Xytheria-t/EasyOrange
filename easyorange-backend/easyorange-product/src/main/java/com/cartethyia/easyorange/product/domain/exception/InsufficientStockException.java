package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;

public class InsufficientStockException extends BaseBusinessException {

    public InsufficientStockException(String message, ProductId productId, StockQuantity currentStock) {
        super(
                ProductResultCode.PRODUCT_OUT_OF_STOCK,
                message + " (productId=" + (productId != null ? productId.value() : "null") + ", stock="
                        + (currentStock != null ? currentStock.value() : "null") + ")");
    }
}
