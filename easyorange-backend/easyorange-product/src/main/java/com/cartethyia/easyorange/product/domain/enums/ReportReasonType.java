package com.cartethyia.easyorange.product.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReasonType {

    FAKE_INFO("1", "虚假信息"),
    INFRINGEMENT("2", "侵权投诉"),
    VIOLATION("3", "违规内容"),
    OTHER("4", "其他");

    @JsonValue
    private final String code;
    private final String desc;

    public static ReportReasonType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("ReportReasonType code must not be null");
        }
        for (var type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ReportReasonType code: " + code);
    }

    public static boolean isValidCode(String code) {
        if (code == null) return false;
        for (var type : values()) {
            if (type.code.equals(code)) return true;
        }
        return false;
    }
}
