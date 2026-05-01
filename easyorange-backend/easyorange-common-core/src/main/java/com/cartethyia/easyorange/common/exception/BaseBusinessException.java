package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

@Getter
public abstract class BaseBusinessException extends RuntimeException {

    private final String code;

    protected abstract String defaultCode();

    protected BaseBusinessException(String message) {
        super(message);
        this.code = defaultCode();
    }

    protected BaseBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    public int httpStatus() {
        return IResultCode.mapToHttpStatus(code);
    }
}
