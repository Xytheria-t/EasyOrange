package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * product 模块错误码
 * <p>
 * 错误码范围：B2001-B2999。HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum ProductResultCode implements IResultCode {
    PRODUCT_NOT_FOUND("B2001", "资产不存在"),
    PRODUCT_OFF_SHELF("B2002", "资产已下架"),
    PRODUCT_OUT_OF_STOCK("B2003", "资产库存不足"),
    PRODUCT_ALREADY_SOLD("B2004", "资产已售出"),
    PRODUCT_NOT_OWNER("B2005", "非资产所有者"),
    PRODUCT_REVIEWED("B2006", "资产已审核"),
    REPORT_NOT_FOUND("B2007", "举报记录不存在"),
    REPORT_ERROR("B2008", "举报业务异常"),
    PRODUCT_STATUS_INVALID("B2009", "资产状态不合法"),
    RATING_NOT_FOUND("B2010", "评价不存在"),
    RATING_NOT_OWNER("B2011", "非评价作者"),
    INVALID_CONDITION_LEVEL("B2012", "成色等级不合法"),
    INVALID_REPORT_TYPE("B2013", "举报类型不合法"),
    REPORT_DUPLICATE("B2014", "重复举报"),
    REPORT_NOT_OWNER("B2015", "非举报作者");

    private final String code;
    private final String message;
}
