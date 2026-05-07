package com.cartethyia.easyorange.user.exception;

/**
 * 模块私有业务异常
 * <p>
 * 用于 easyorange-user 模块内部的业务校验失败场景，
 * 区别于 com.cartethyia.easyorange.common.exception.BusinessException，
 * 该异常携带模块特有的错误码语义。
 */
public class UserBizException extends RuntimeException {

    private final String code;

    public UserBizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UserBizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
