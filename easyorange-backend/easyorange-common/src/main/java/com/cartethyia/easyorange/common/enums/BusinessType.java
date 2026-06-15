package com.cartethyia.easyorange.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nullable;
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

    OTHER(0, "其它"),
    ADD(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除"),
    LOGIN(4, "登录");

    private static final Map<Integer, BusinessType> CODE_MAP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(e -> e.code, e -> e));

    private final int code;
    private final String desc;

    @JsonValue
    public String getDesc() {
        return desc;
    }

    @Nullable
    public static BusinessType fromCode(int code) {
        return CODE_MAP.get(code);
    }

    /**
     * 从 code 值反序列化为枚举（支持 JSON 反序列化），未匹配时返回 OTHER。
     */
    @JsonCreator
    public static BusinessType fromJsonValue(int code) {
        var result = fromCode(code);
        return result != null ? result : OTHER;
    }
}
