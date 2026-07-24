package com.cartethyia.easyorange.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 业务类型枚举，用于操作日志标识操作类型。
 *
 * @author cartethyia
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    OTHER("0", "其它"),
    ADD("1", "新增"),
    UPDATE("2", "修改"),
    DELETE("3", "删除"),
    LOGIN("4", "登录");

    private static final Map<String, BusinessType> CODE_MAP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(e -> e.code, e -> e));

    @JsonValue
    private final String code;
    private final String desc;

    public static BusinessType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("BusinessType code must not be null");
        }
        var result = CODE_MAP.get(code);
        if (result == null) {
            throw new IllegalArgumentException("Unknown BusinessType code: " + code);
        }
        return result;
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
