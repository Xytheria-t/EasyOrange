package com.cartethyia.easyorange.message.enums;

import java.util.Arrays;
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
public enum ReadStatus {

    UNREAD(0, "未读"),
    READ(1, "已读");

    private final Integer code;
    private final String desc;

    public static ReadStatus fromCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public static String getDescByCode(Integer code) {
        ReadStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
