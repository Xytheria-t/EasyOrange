package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;

public class BusinessException extends BaseBusinessException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(IResultCode resultCode) {
        super(resultCode);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public BusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    // Static factory methods for concise throw-site usage
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(IResultCode resultCode) {
        return new BusinessException(resultCode);
    }

    public static BusinessException of(String message, Throwable cause) {
        return new BusinessException(message, cause);
    }

    public static BusinessException of(IResultCode resultCode, String message) {
        return new BusinessException(resultCode, message);
    }

    public static BusinessException of(IResultCode resultCode, String message, Throwable cause) {
        return new BusinessException(resultCode, message, cause);
    }
}
