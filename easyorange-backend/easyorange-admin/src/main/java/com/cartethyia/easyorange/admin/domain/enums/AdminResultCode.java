package com.cartethyia.easyorange.admin.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * admin 模块错误码
 * <p>
 * 错误码范围：B6xxx（[架构参考](../../../doc/agents/架构参考.md) 里的预留段，此前只有文档没有实现）。
 * HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * <p>
 * admin 之前全部业务异常走 {@code BusinessException.of("中文串")}，code 落到通用默认 ——
 * 「用户不存在」与「订单不存在」对外是同一个码，前端没法分支。
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum AdminResultCode implements IResultCode {
    USER_NOT_FOUND("B6001", "用户不存在"),
    ORDER_NOT_FOUND("B6002", "订单不存在"),
    PRODUCT_NOT_FOUND("B6003", "商品不存在"),
    CATEGORY_NOT_FOUND("B6004", "分类不存在"),
    AUDIT_LOG_NOT_FOUND("B6005", "审核记录不存在");

    private final String code;
    private final String message;
}
