package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;

public class ProductNotOwnerException extends BaseBusinessException {

    public ProductNotOwnerException(ProductId productId) {
        super(ProductResultCode.PRODUCT_NOT_OWNER, "无权操作此资产: id=" + (productId != null ? productId.value() : "null"));
    }
}
