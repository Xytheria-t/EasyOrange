package com.cartethyia.easyorange.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用结果码枚举
 * <p>
 * 前缀规则：A = 成功/认证授权 | B = 业务错误 | C = 服务端错误
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode implements IResultCode {

    // A: 成功 / 认证授权
    SUCCESS("A0000", "成功"),
    UNAUTHORIZED("A0401", "未登录"),
    FORBIDDEN("A0403", "禁止访问"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不允许"),

    // B: 业务错误
    FAIL("B0001", "操作失败"),
    BUSINESS_ERROR("B0002", "业务异常"),
    VALIDATE_FAILED("B0003", "参数校验失败"),
    PARAM_ERROR("B0004", "参数错误"),
    CONCURRENT_UPDATE("B0006", "并发更新冲突"),

    // C: 服务端错误
    INTERNAL_SERVER_ERROR("C0500", "服务器内部错误"),
    SERVICE_UNAVAILABLE("C0503", "服务不可用");

    private final String code;
    private final String message;
}
