package com.cartethyia.easyorange.common.exception.file;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;

public class FileException extends BaseBusinessException {

    public static FileException of(String message) {
        return new FileException(message);
    }

    public static FileException of(String message, Throwable cause) {
        return new FileException(message, cause);
    }

    public static FileException of(IResultCode resultCode) {
        return new FileException(resultCode);
    }

    public static FileException of(IResultCode resultCode, String message) {
        return new FileException(resultCode, message);
    }

    public static FileException of(IResultCode resultCode, String message, Throwable cause) {
        return new FileException(resultCode, message, cause);
    }

    public FileException(IResultCode resultCode) {
        super(resultCode);
    }

    public FileException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public FileException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    public FileException(String message) {
        super(message);
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected String defaultCode() {
        return FileResultCode.FILE_UPLOAD_FAILED.getCode();
    }
}