package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * product 模块错误码
 * <p>
 * 错误码范围：B2001-B2999。HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 * <p>
 * 码值空洞（B2002 / B2004 / B2006）是刻意的：那些曾是「已下架 / 已售出 / 已审核」的按状态分列的码，
 * 但状态拒绝统一走 {@link #PRODUCT_STATUS_INVALID}（携带当前状态辅助定位状态机误用），
 * 旧码从未被引用，已删除。已删除的码值不再复用。
 * </p>
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum ProductResultCode implements IResultCode {
    PRODUCT_NOT_FOUND("B2001", "资产不存在"),
    PRODUCT_OUT_OF_STOCK("B2003", "资产库存不足"),
    PRODUCT_NOT_OWNER("B2005", "非资产所有者"),
    REPORT_NOT_FOUND("B2007", "举报记录不存在"),
    REPORT_ERROR("B2008", "举报业务异常"),
    PRODUCT_STATUS_INVALID("B2009", "资产状态不合法"),
    RATING_NOT_FOUND("B2010", "评价不存在"),
    RATING_NOT_OWNER("B2011", "非评价作者"),
    INVALID_CONDITION_LEVEL("B2012", "成色等级不合法"),
    INVALID_REPORT_TYPE("B2013", "举报类型不合法"),
    REPORT_DUPLICATE("B2014", "重复举报"),
    REPORT_NOT_OWNER("B2015", "非举报作者"),
    RATING_ORDER_NOT_COMPLETED("B2016", "仅可评价已完成订单中的资产"),
    RATING_ALREADY_EXISTS("B2017", "该订单已评价"),
    RATING_ORDER_REQUIRED("B2018", "评价必须绑定订单"),
    PRODUCT_ERROR("B2019", "资产业务异常");

    private final String code;
    private final String message;
}
