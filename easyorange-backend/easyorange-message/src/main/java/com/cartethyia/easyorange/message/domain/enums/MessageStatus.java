package com.cartethyia.easyorange.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举 —— 对应 {@code eo_message.msg_status} 列（VARCHAR(20) 语义串）。
 * <p>
 * 只保留实际写入的两个状态：SENT（落库即已发送）→ RECALLED（2 分钟内撤回）。
 * 「未读 / 已读」不由本列承载，而是 {@code is_read} 列 + {@link ReadStatus}；
 * 「已送达」没有采集点（站内信送达与否由客户端拉列表体现），故不列在此。
 * <p>
 * 边界：V1 的 CHECK 仍允许五个取值（含已删的三个），属 DDL 与代码的已知差异；
 * 收口收表时再由迁移收紧约束，不在本模块改已执行脚本。
 */
@Getter
@AllArgsConstructor
public enum MessageStatus implements BaseCodeEnum {
    SENT("SENT", "已发送"),
    RECALLED("RECALLED", "已撤回");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static MessageStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(MessageStatus.class, code);
    }
}
