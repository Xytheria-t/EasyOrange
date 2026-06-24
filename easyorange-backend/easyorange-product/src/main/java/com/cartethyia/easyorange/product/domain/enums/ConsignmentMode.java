package com.cartethyia.easyorange.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConsignmentMode {

    MANUAL(0, "手动管理"),
    AI_MANAGED(1, "AI托管");

    private final Integer code;
    private final String desc;

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@link #MANUAL} if code is null or not recognized
     */
    public static ConsignmentMode fromCode(Integer code) {
        if (code == null) {
            return MANUAL;
        }
        for (ConsignmentMode mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return MANUAL;
    }
}
