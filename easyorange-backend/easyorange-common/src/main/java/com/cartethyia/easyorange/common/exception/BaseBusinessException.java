package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import lombok.Getter;

@Getter
public abstract class BaseBusinessException extends RuntimeException {

    private final String code;

    protected BaseBusinessException(String message) {
        super(message);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    protected String defaultCode() {
        return ResultCode.BUSINESS_ERROR.getCode();
    }
}
