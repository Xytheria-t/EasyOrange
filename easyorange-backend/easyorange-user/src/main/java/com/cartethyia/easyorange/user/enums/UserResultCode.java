package com.cartethyia.easyorange.user.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

/**
 * 用户模块错误码
 * <p>
 * 错误码范围：B1001-B1999
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
public enum UserResultCode implements IResultCode {

    USER_NOT_FOUND("B1001", "用户不存在"),
    USER_DISABLED("B1002", "账户已被禁用"),
    USER_LOCKED("B1003", "账户已被锁定"),
    USERNAME_EXISTS("B1004", "用户名已存在"),
    EMAIL_EXISTS("B1005", "邮箱已被注册"),
    PHONE_EXISTS("B1006", "手机号已被注册"),
    PASSWORD_ERROR("B1007", "密码错误");

    private final String code;
    private final String message;

    UserResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
