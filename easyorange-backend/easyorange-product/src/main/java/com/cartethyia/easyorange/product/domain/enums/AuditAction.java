package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
import java.util.Arrays;

/**
 * 资产审核动作枚举。
 * <p>
 * 替代审核流程中的魔法数（1=通过, 2=拒绝, 3=重提交）。
 */
public enum AuditAction {

    APPROVED(1, "通过"),
    REJECTED(2, "拒绝"),
    RESUBMIT(3, "重提交");

    private final int code;
    private final String desc;

    AuditAction(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@code null} if code is null or not recognized
     */
    @Nullable
    public static AuditAction fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElse(null);
    }

    public static String getDescByCode(Integer code) {
        AuditAction action = fromCode(code);
        return action != null ? action.getDesc() : "未知";
    }
}
