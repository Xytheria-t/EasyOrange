package com.cartethyia.easyorange.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ResultCode implements IResultCode {

    SUCCESS("A0000", "成功"),

    FAIL("B0001", "操作失败"),

    BUSINESS_ERROR("B0002", "业务异常"),

    VALIDATE_FAILED("A0400", "参数校验失败"),

    UNAUTHORIZED("A0401", "未登录"),

    TOKEN_EXPIRED("A0402", "登录已过期"),

    FORBIDDEN("A0403", "没有权限"),

    METHOD_NOT_ALLOWED("A0405", "请求方法不支持"),

    NOT_FOUND("A0404", "请求不存在"),

    INTERNAL_SERVER_ERROR("C0500", "服务器内部错误");

    private static final Map<String, ResultCode> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ResultCode::getCode, Function.identity()));

    private final String code;
    private final String message;

    @Override
    @JsonValue
    public String getCode() {
        return code;
    }

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static ResultCode fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
