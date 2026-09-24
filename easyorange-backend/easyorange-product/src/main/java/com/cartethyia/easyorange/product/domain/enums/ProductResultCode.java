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
 * 码值空洞（B2002 / B2004 / B2006 / B2007 / B2008 / B2010 / B2011 / B2013 / B2014 / B2015 / B2016 / B2017 / B2018）是刻意的：
 * 前三个曾是「已下架 / 已售出 / 已审核」的按状态分列的码，但状态拒绝统一走
 * {@link #PRODUCT_STATUS_INVALID}（携带当前状态辅助定位状态机误用）；B2010/B2011/B2016-B2018 属已下线的商品评价，
 * B2013-B2015 属已下线的举报功能。这些码从未被外部契约引用，已删除。已删除的码值不再复用。
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
    PRODUCT_STATUS_INVALID("B2009", "资产状态不合法"),
    INVALID_CONDITION_LEVEL("B2012", "成色等级不合法"),
    PRODUCT_ERROR("B2019", "资产业务异常");

    private final String code;
    private final String message;
}
