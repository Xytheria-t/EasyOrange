package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;

public class RatingNotFoundException extends BaseBusinessException {

    public RatingNotFoundException(String reviewId) {
        super(ProductResultCode.RATING_NOT_FOUND, "评价不存在 (reviewId=" + reviewId + ")");
    }
}
