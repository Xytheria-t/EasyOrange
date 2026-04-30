package com.cartethyia.easyorange.product.application.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

/**
 * 商品模块错误码
 * <p>
 * 错误码范围：B2001-B2999
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
public enum ProductResultCode implements IResultCode {

    PRODUCT_NOT_FOUND("B2001", "商品不存在"),
    PRODUCT_OFF_SHELF("B2002", "商品已下架"),
    PRODUCT_OUT_OF_STOCK("B2003", "商品库存不足"),
    PRODUCT_ALREADY_SOLD("B2004", "商品已售出"),
    PRODUCT_NOT_OWNER("B2005", "非商品所有者"),
    PRODUCT_REVIEWED("B2006", "商品已审核");

    private final String code;
    private final String message;

    ProductResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
