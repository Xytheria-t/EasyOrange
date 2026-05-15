package com.cartethyia.easyorange.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 * <p>
 * 用于操作日志注解中标识操作类型。
 * 查找方法请使用 {@link com.cartethyia.easyorange.common.util.EnumUtils#fromCodeSafe}。
 * </p>
 *
 * @author cartethyia
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    /**
     * 其它
     */
    OTHER(0, "其它"),

    /**
     * 新增
     */
    ADD(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 登录
     */
    LOGIN(4, "登录");

    private final int code;

    private final String desc;

    @JsonValue
    public String getDesc() {
        return desc;
    }

    /**
     * 从 code 值查找枚举
     */
    public static BusinessType fromCode(int code) {
        for (BusinessType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    /**
     * 从 code 值反序列化为枚举（支持 JSON 反序列化）
     */
    @JsonCreator
    public static BusinessType fromJsonValue(int code) {
        BusinessType result = fromCode(code);
        return result != null ? result : OTHER;
    }
}
