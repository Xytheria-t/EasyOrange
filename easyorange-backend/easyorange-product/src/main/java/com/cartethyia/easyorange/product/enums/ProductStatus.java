package com.cartethyia.easyorange.product.enums;

import com.cartethyia.easyorange.common.util.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {

    DRAFT(0, "草稿"),
    ONLINE(1, "上架"),
    SOLD(2, "已售出"),
    OFFLINE(3, "下架");

    private final Integer code;
    private final String desc;

    public static ProductStatus fromCode(Integer code) {
        return EnumUtils.fromCodeSafe(code, values(), ProductStatus::getCode).orElse(null);
    }

    public static String getDescByCode(Integer code) {
        ProductStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
