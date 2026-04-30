package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;

public class InvalidProductStatusException extends RuntimeException {

    public InvalidProductStatusException(String message, ProductId productId, ProductStatusVO currentStatus) {
        super(message + " (productId=" + (productId != null ? productId.value() : "null")
                + ", currentStatus=" + (currentStatus != null ? currentStatus.value() : "null") + ")");
    }

    public InvalidProductStatusException(String message, ProductStatusVO currentStatus) {
        super(message + " (currentStatus=" + (currentStatus != null ? currentStatus.value() : "null") + ")");
    }
}
