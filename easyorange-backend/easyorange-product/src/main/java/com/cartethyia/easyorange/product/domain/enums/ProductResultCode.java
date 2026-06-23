package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductResultCode implements IResultCode {

    PRODUCT_NOT_FOUND("B2001", "商品不存在"),
    PRODUCT_OFF_SHELF("B2002", "商品已下架"),
    PRODUCT_OUT_OF_STOCK("B2003", "商品库存不足"),
    PRODUCT_ALREADY_SOLD("B2004", "商品已售出"),
    PRODUCT_NOT_OWNER("B2005", "非商品所有者"),
    PRODUCT_REVIEWED("B2006", "商品已审核"),
    REPORT_NOT_FOUND("B2007", "举报记录不存在"),
    REPORT_ERROR("B2008", "举报业务异常"),
    PRODUCT_STATUS_INVALID("B2009", "商品状态不合法");

    private final String code;
    private final String message;
}
