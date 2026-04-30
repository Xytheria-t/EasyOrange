package com.cartethyia.easyorange.message.enums;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageStatus {

    UNREAD(0, "未读"),
    READ(1, "已读");

    private final Integer code;
    private final String desc;

    public static MessageStatus fromCode(Integer code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

    public static String getDescByCode(Integer code) {
        MessageStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
