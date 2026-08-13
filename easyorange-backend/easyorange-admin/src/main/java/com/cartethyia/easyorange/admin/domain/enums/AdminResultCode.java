package com.cartethyia.easyorange.admin.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * admin 模块错误码
 * <p>
 * 错误码范围：B6001-B6999。HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum AdminResultCode implements IResultCode {
    REPORT_NOT_FOUND("B6001", "举报记录不存在"),
    REPORT_ALREADY_HANDLED("B6002", "该举报已被处理"),
    REPORT_INVALID_ACTION("B6003", "无效的处理动作"),
    REPORT_LIST_EMPTY("B6004", "举报ID列表不能为空"),
    REPORT_BATCH_LIMIT_EXCEEDED("B6005", "批量处理数量不能超过50条"),
    REPORT_PRODUCT_NOT_FOUND("B6006", "关联商品不存在"),
    RATING_NOT_FOUND("B6007", "评价不存在"),
    RATING_NOT_FOUND_OR_DELETED("B6008", "评价不存在或已被删除");

    private final String code;
    private final String message;
}
