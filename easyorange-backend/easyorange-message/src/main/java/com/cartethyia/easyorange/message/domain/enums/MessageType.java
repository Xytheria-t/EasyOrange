package com.cartethyia.easyorange.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举 —— 对应 {@code eo_message.type} 列（TINYINT，存 code 的整数值）。
 * <p>
 * 前端按 code 数字收发（WS 发 type:2、REST 按 type=1 查系统通知），故 JSON 侧 {@code @JsonValue} 在 code 上；
 * 领域内部一律用枚举，非法 code 在边界 {@code fromCode} 抛异常映射 400，不静默落库。
 */
@Getter
@AllArgsConstructor
public enum MessageType implements BaseCodeEnum {
    SYSTEM("1", "系统通知"),
    CHAT("2", "聊天消息"),
    ORDER("3", "订单消息"),
    PAYMENT("4", "支付消息"),
    ACTIVITY("5", "活动通知");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static MessageType fromCode(String code) {
        return BaseCodeEnum.fromCode(MessageType.class, code);
    }
}
