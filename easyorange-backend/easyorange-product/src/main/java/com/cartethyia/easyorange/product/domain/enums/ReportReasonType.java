package com.cartethyia.easyorange.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReasonType {

    FAKE_INFO(1, "虚假信息"),
    INFRINGEMENT(2, "侵权投诉"),
    VIOLATION(3, "违规内容"),
    OTHER(4, "其他");

    private final Integer code;
    private final String desc;

    public static ReportReasonType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReportReasonType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
