package com.cartethyia.easyorange.user.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserResultCode implements IResultCode {
    USER_NOT_FOUND("B1001", "用户不存在"),
    USER_DISABLED("B1002", "账户已被禁用"),
    USER_LOCKED("B1003", "账户已被锁定"),
    USERNAME_EXISTS("B1004", "用户名已存在"),
    EMAIL_EXISTS("B1005", "邮箱已被注册"),
    PHONE_EXISTS("B1006", "手机号已被注册"),
    STUDENT_ID_EXISTS("B1012", "学号已被注册"),
    PASSWORD_ERROR("B1007", "密码错误"),
    INVALID_CREDENTIALS("B1011", "账号或密码错误"),
    SMS_CODE_INVALID("B1008", "验证码无效或已过期"),
    SMS_CODE_SEND_TOO_FREQUENT("B1009", "验证码发送过于频繁"),
    SMS_CODE_VERIFY_TOO_FREQUENT("B1010", "验证码验证次数过多，请重新获取");

    @JsonValue
    private final String code;
    private final String message;
}