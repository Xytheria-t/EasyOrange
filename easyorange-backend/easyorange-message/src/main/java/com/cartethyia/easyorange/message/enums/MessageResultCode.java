package com.cartethyia.easyorange.message.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息模块错误码
 * <p>
 * 错误码范围：B7001-B7999
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
    TEMPLATE_NOT_FOUND("B7003", "消息模板不存在"),
    TEMPLATE_CODE_DUPLICATE("B7004", "模板编码已存在"),
    TEMPLATE_DISABLED("B7005", "消息模板已禁用"),
    TEMPLATE_RENDER_ERROR("B7006", "模板渲染失败"),
    TEMPLATE_VARIABLE_MISSING("B7007", "模板变量缺失"),
    MESSAGE_DOMAIN_ERROR("B7008", "消息业务异常");

    private final String code;
    private final String message;
}
