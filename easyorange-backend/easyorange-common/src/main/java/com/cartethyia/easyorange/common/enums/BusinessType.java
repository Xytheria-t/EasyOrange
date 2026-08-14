package com.cartethyia.easyorange.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举，用于操作日志标识操作类型。
 *
 * @author cartethyia
 */
@Getter
@AllArgsConstructor
public enum BusinessType implements BaseCodeEnum {
    OTHER("0", "其它"),
    ADD("1", "新增"),
    UPDATE("2", "修改"),
    DELETE("3", "删除"),
    LOGIN("4", "登录");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static BusinessType fromCode(String code) {
        return BaseCodeEnum.fromCode(BusinessType.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
