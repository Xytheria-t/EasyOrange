package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
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

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@code null} if code is null
     * @throws IllegalArgumentException if code is non-null but not recognized
     */
    @Nullable
    public static ReportReasonType fromCode(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 1 -> FAKE_INFO;
            case 2 -> INFRINGEMENT;
            case 3 -> VIOLATION;
            case 4 -> OTHER;
            default -> throw new IllegalArgumentException("Unknown ReportReasonType code: " + code);
        };
    }

    public static boolean isValidCode(Integer code) {
        if (code == null) return false;
        for (var type : values()) {
            if (type.getCode().equals(code)) return true;
        }
        return false;
    }
}
