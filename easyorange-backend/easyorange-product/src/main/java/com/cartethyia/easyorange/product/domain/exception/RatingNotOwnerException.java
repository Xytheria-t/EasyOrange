package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;

public class RatingNotOwnerException extends BaseBusinessException {

    public RatingNotOwnerException(String reviewId) {
        super(ProductResultCode.RATING_NOT_OWNER, "只能删除自己的评价 (reviewId=" + reviewId + ")");
    }
}
