package com.cartethyia.easyorange.payment.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道 / 资金侧支付状态机 — 保留支付中、部分退款等渠道细节。与 order 模块的
 * {@code com.cartethyia.easyorange.order.domain.enums.PaymentStatus} 是两套独立状态机：
 * 订单侧只投影 UNPAID / PAID / REFUNDED（本枚举的 SUCCESS 在订单侧叫 PAID），不共享类型。
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus implements BaseCodeEnum {
    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "已支付"),
    REFUNDED("REFUNDED", "已退款"),
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "部分退款"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭"),
    PAYING("PAYING", "支付中"),
    REFUNDING("REFUNDING", "退款中");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static PaymentStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(PaymentStatus.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
