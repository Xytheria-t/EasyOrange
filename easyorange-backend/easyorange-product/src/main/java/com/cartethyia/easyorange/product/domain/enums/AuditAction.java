package com.cartethyia.easyorange.product.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
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

    APPROVED("1", "通过"),
    REJECTED("2", "拒绝"),
    RESUBMIT("3", "重提交");

    @JsonValue
    private final String code;
    private final String desc;

    public static AuditAction fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("AuditAction code must not be null");
        }
        for (var action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown AuditAction code: " + code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
