package com.cartethyia.easyorange.order.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单侧支付状态枚举 — 与 payment 模块的 {@code PaymentStatus} 是两套独立状态机，
 * 不共享类型：订单只关心「能不能付 / 能不能退」，渠道侧状态细节不外泄。
 * <p>
 * code 为有意义字符串，DB 列 VARCHAR(20)，经 {@code @EnumValue} 持久化。
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus implements BaseCodeEnum {

    /** 未支付 */
    UNPAID("UNPAID", "未支付"),

    /** 已支付 */
    PAID("PAID", "已支付"),

    /** 已退款 */
    REFUNDED("REFUNDED", "已退款");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static PaymentStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(PaymentStatus.class, code);
    }
}
