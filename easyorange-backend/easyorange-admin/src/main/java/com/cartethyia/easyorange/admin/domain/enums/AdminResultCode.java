package com.cartethyia.easyorange.admin.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * admin 模块错误码
 * <p>
 * 错误码范围：B6001-B6999。HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 * <p>
 * 码值空洞（B6001-B6006）是刻意的：那六个码属于已下线的举报后台功能，已删除的码值不再复用。
 * </p>
 *
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum AdminResultCode implements IResultCode {
    RATING_NOT_FOUND("B6007", "评价不存在"),
    RATING_NOT_FOUND_OR_DELETED("B6008", "评价不存在或已被删除");

    private final String code;
    private final String message;
}
