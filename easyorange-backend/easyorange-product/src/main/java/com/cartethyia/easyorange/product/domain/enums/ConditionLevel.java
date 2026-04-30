package com.cartethyia.easyorange.product.domain.enums;

public enum ConditionLevel {

    NEW(1, "全新"),
    LIKE_NEW(2, "几乎全新"),
    GOOD(3, "轻微使用痕迹"),
    FAIR(4, "明显使用痕迹");

    private final Integer code;
    private final String desc;

    ConditionLevel(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ConditionLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConditionLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
