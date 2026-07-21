package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产审核动作枚举。
 * <p>
 * 替代审核流程中的魔法数（1=通过, 2=拒绝, 3=重提交）。
 */
@Getter
@AllArgsConstructor
public enum AuditAction {

    APPROVED(1, "通过"),
    REJECTED(2, "拒绝"),
    RESUBMIT(3, "重提交");

    private final int code;
    private final String desc;

    @Nullable
    public static AuditAction fromCode(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 1 -> APPROVED;
            case 2 -> REJECTED;
            case 3 -> RESUBMIT;
            default -> throw new IllegalArgumentException("Unknown AuditAction code: " + code);
        };
    }

    public static String getDescByCode(Integer code) {
        try {
            var action = fromCode(code);
            return action != null ? action.getDesc() : "未知";
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
