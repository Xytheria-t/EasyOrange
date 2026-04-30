package com.cartethyia.easyorange.product.domain.enums;

import java.util.Arrays;

public enum ProductStatus {

    DRAFT(0, "草稿"),
    ONLINE(1, "上架"),
    SOLD(2, "已售出"),
    OFFLINE(3, "下架");

    private final Integer code;
    private final String desc;

    ProductStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProductStatus fromCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public static String getDescByCode(Integer code) {
        ProductStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
