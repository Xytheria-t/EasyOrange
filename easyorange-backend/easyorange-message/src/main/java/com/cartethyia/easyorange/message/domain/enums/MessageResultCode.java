package com.cartethyia.easyorange.message.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息模块错误码
 * <p>
 * 错误码范围：B7001-B7999
 * </p>
 * <p>
 * 码值空洞（B7003-B7007）是刻意的：那是「消息模板不存在 / 编码重复 / 已禁用 / 渲染失败 / 变量缺失」
 * 五个模板类错误的码，但 message 模块从未实现模板功能，从未被引用，已删除。已删除的码值不再复用。
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum MessageResultCode implements IResultCode {
    MESSAGE_NOT_FOUND("B7001", "消息不存在"),
    MESSAGE_NOT_OWNER("B7002", "非消息接收者"),
    MESSAGE_DOMAIN_ERROR("B7008", "消息业务异常");

    private final String code;
    private final String message;
}
