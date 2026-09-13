package com.cartethyia.easyorange.product.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存流水变更类型 — 每次库存变更必须落一种，是「这笔变动从哪来」的唯一标识。
 * <p>
 * 幂等粒度由 {@code (changeType, bizId, productId)} 唯一索引决定：{@link #DECREASE} / {@link #RESTORE}
 * 携带订单号，因此同一订单对同一资产只会落账一次；{@link #INIT} / {@link #ADJUST} 无业务单号，
 * 不参与幂等约束（同一资产可多次人工调整）。
 */
@Getter
@AllArgsConstructor
public enum StockChangeType implements BaseCodeEnum {

    /** 资产创建时的库存基线 */
    INIT("INIT", "初始化"),

    /** 下单扣减（同事务内与订单一起落库） */
    DECREASE("DECREASE", "下单扣减"),

    /** 取消 / 退款恢复 */
    RESTORE("RESTORE", "库存恢复"),

    /** 卖家或管理端直接改库存 */
    ADJUST("ADJUST", "人工调整");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
