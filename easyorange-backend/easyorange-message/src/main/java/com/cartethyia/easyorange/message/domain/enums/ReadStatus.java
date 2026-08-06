package com.cartethyia.easyorange.message.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息读取状态枚举
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Getter
@AllArgsConstructor
public enum ReadStatus implements BaseCodeEnum {
    UNREAD("0", "未读"),
    READ("1", "已读");

    @JsonValue
    private final String code;

    private final String desc;

    public static ReadStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ReadStatus.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
