package com.cartethyia.easyorange.payment.enums;

import com.cartethyia.easyorange.common.util.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {

    PENDING(0, "待支付"),
    SUCCESS(1, "已支付"),
    REFUNDED(2, "已退款"),
    FAILED(3, "支付失败"),
    CLOSED(4, "已关闭");

    private final Integer code;
    private final String desc;

    public static PaymentStatus fromCode(Integer code) {
        return EnumUtils.fromCodeSafe(code, values(), PaymentStatus::getCode).orElse(null);
    }

    public static String getDescByCode(Integer code) {
        PaymentStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }

    public static boolean canPay(Integer status) {
        return PENDING.getCode().equals(status);
    }

    public static boolean canRefund(Integer status) {
        return SUCCESS.getCode().equals(status);
    }

    public static boolean canClose(Integer status) {
        return PENDING.getCode().equals(status) || FAILED.getCode().equals(status);
    }
}
