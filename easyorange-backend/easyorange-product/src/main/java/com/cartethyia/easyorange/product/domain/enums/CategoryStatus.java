package com.cartethyia.easyorange.product.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分类启用状态 — 0 禁用 / 1 启用（对应 {@code eo_category.status} 的 TINYINT）。
 * <p>
 * 此前 status 是裸 {@code Integer}，三条读路径各自决定要不要过滤，且只有一处过滤对了。
 * 改成枚举 + 单一口径后，"禁用即不出现"是类型系统保证的。
 */
@Getter
@AllArgsConstructor
public enum CategoryStatus implements BaseCodeEnum {
    DISABLED("0", "禁用"),
    ENABLED("1", "启用");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static CategoryStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(CategoryStatus.class, code);
    }

    /** 是否启用。 */
    public boolean isEnabled() {
        return this == ENABLED;
    }
}
