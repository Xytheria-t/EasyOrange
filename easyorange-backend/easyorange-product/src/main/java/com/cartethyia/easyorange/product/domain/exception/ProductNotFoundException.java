package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.common.domain.ProductId;

public class ProductNotFoundException extends BaseBusinessException {

    public ProductNotFoundException(ProductId id) {
        super(ProductResultCode.PRODUCT_NOT_FOUND, "资产不存在: id=" + (id != null ? id.value() : "null"));
    }

    public ProductNotFoundException(String id) {
        super(ProductResultCode.PRODUCT_NOT_FOUND, "资产不存在: id=" + id);
    }
}
