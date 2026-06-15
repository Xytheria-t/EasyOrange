package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

public class InvalidProductStatusException extends BaseBusinessException {

    public InvalidProductStatusException(String message, ProductId productId, ProductStatus currentStatus) {
        super(message + " (productId=" + (productId != null ? productId.value() : "null")
                + ", currentStatus=" + (currentStatus != null ? currentStatus : "null") + ")");
    }

    public InvalidProductStatusException(String message, ProductStatus currentStatus) {
        super(message + " (currentStatus=" + (currentStatus != null ? currentStatus : "null") + ")");
    }
}
