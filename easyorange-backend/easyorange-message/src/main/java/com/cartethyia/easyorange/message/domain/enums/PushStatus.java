package com.cartethyia.easyorange.message.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 离线消息推送状态枚举 —— 对应 {@code eo_offline_message.push_status} 列（TINYINT 0/1/2）。
 * <p>
 * code 存的是数字字符串（"0"/"1"/"2"）而非语义名：列是 TINYINT，{@code MessageDataMapper} 在持久层
 * 边界做 String ↔ int 转换，领域内其余部分只见枚举。
 * <p>
 * 边界：FAILED 目前无生产写入方（补推时目标消息缺失或非系统通知就跳过、保持 PENDING，等用户下次上线再试），
 * 保留该值只为能读懂这类行；本枚举无 {@code @EnumValue}——{@code OfflineMessageDO.pushStatus} 声明为
 * {@code Integer}（列是 TINYINT），转换在 {@code MessageDataMapper} 边界显式做。
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
}
