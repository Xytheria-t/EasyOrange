package com.cartethyia.easyorange.message.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送状态枚举 —— 对应 {@code eo_offline_message.push_status} 列（TINYINT，0/1/2）。
 * <p>
 * 沿用 MessageType 对 TINYINT 的处理：code 存语义串（"0"/"1"/"2"），
 * 由持久层边界做 String ↔ int 转换，替换原 MessageConstant.PUSH_STATUS_* 裸 int。
 */
@Getter
@AllArgsConstructor
public enum PushStatus implements BaseCodeEnum {
    PENDING("0", "待推送"),
    PUSHED("1", "已推送"),
    FAILED("2", "推送失败");

    @JsonValue
    private final String code;

    private final String desc;

    public static PushStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(PushStatus.class, code);
    }

    public static String getDescByCode(String code) {
        try {
            return fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }
}
