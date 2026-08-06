package com.cartethyia.easyorange.admin.domain.enums;

import com.cartethyia.easyorange.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportHandleAction {
    RESOLVE("resolve", "处理通过", true),
    DISMISS("dismiss", "驳回", false),
    IGNORE("IGNORE", "忽略", false),
    PRODUCT_OFFLINE("PRODUCT_OFFLINE", "下架商品", true),
    WARN_SENDER("WARN_SENDER", "警告举报人", false),
    BAN_PRODUCT("BAN_PRODUCT", "封禁商品", true);

    private final String code;
    private final String desc;
    private final boolean resolves;

    public static ReportHandleAction fromCode(String code) {
        ReportHandleAction action = fromCodeOrNull(code);
        if (action == null) {
            throw BusinessException.of(AdminResultCode.REPORT_INVALID_ACTION);
        }
        return action;
    }

    public static ReportHandleAction fromCodeOrNull(String code) {
        if (code == null) {
            return null;
        }
        for (var action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }

    /**
     * 处理结果描述：resolve/dismiss/IGNORE 在 remark 为空时使用默认文案，
     * 其余动作在 remark 前拼接动作前缀。
     */
    public String describe(String remark) {
        return switch (this) {
            case RESOLVE -> blankToDefault(remark, "举报已处理");
            case DISMISS -> blankToDefault(remark, "举报已驳回");
            case IGNORE -> blankToDefault(remark, "管理员忽略");
            case PRODUCT_OFFLINE -> "下架商品: " + value(remark);
            case WARN_SENDER -> "警告举报人: " + value(remark);
            case BAN_PRODUCT -> "封禁商品: " + value(remark);
        };
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
