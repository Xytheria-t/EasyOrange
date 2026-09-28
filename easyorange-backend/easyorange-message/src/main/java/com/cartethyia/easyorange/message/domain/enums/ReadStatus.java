package com.cartethyia.easyorange.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息已读状态枚举 —— 对应 {@code eo_message.is_read} 列（TINYINT 0/1）。
 * <p>
 * 与 {@link MessageStatus} 分工：已读与否只由本列表达（批量标记走一条 UPDATE 的谓词下推），
 * {@code msg_status} 只管发送与撤回，两者不交叉。
 */
@Getter
@AllArgsConstructor
public enum ReadStatus implements BaseCodeEnum {
    UNREAD("0", "未读"),
    READ("1", "已读");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static ReadStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ReadStatus.class, code);
    }
}
