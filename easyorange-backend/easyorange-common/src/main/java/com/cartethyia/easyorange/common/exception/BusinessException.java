package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;

public class BusinessException extends BaseBusinessException {

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(IResultCode resultCode) {
        super(resultCode);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    protected BusinessException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    protected BusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

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

    @Override
    protected String defaultCode() {
        return ResultCode.BUSINESS_ERROR.getCode();
    }
}