package com.cartethyia.easyorange.message.enums;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageStatus {

    UNREAD(0, "未读"),
    READ(1, "已读"),
    SENT("SENT", "已发送"),
    DELIVERED("DELIVERED", "已送达"),
    RECALLED("RECALLED", "已撤回");

    private final Object code;
    private final String desc;

    @SuppressWarnings("unchecked")
    public <T> T getCode() {
        return (T) code;
    }

    public static MessageStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(v -> v.code instanceof Integer && ((Integer) v.code).equals(code))
                .findFirst()
                .orElse(null);
    }

    public static MessageStatus fromStringCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code instanceof String && ((String) v.code).equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    public static String getDescByCode(Integer code) {
        MessageStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }
}
