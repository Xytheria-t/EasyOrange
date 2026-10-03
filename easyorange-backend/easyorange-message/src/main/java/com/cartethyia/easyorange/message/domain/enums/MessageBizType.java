package com.cartethyia.easyorange.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务对象类型 —— 对应 {@code eo_message.biz_type} 列，决定通知点击后落到哪张页面。
 * <p>
 * {@code business_id} 只带 ID 不带类型，类型缺失时读侧只能靠标题中文猜「跳商品还是跳订单」，
 * 订单类通知会跳到 {@code /products/{orderId}} 落到 404。类型随消息落库，读侧不再猜。
 * <p>
 * {@link #NONE} 是收敛值：聊天消息与无业务对象的系统通知都落它，前端据此不给跳转入口。
 */
@Getter
@AllArgsConstructor
public enum MessageBizType implements BaseCodeEnum {
    NONE("0", "无业务对象"),
    PRODUCT("1", "商品"),
    ORDER("2", "订单");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static MessageBizType fromCode(String code) {
        return BaseCodeEnum.fromCode(MessageBizType.class, code);
    }
}
