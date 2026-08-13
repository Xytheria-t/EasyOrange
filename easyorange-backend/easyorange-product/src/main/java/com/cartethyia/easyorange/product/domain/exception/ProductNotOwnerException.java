package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;

public class ProductNotOwnerException extends BaseBusinessException {

    public ProductNotOwnerException(ProductId productId, String message) {
        super(
                ProductResultCode.PRODUCT_NOT_OWNER,
                message + " (productId=" + (productId != null ? productId.value() : "null") + ")");
    }
}
