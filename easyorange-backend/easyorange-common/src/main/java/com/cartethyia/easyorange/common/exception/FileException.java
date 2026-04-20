package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import com.cartethyia.easyorange.common.enums.IResultCode;

/**
 * 文件异常类
 * <p>
 * 统一使用 {@code of()} 工厂方法创建实例。
 * </p>
 *
 * <pre>{@code
 * // 用法示例
 * throw FileException.of("文件上传失败");
 * throw FileException.of(FileResultCode.FILE_SIZE_EXCEEDED);
 * throw FileException.of(FileResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持 JPG、PNG 格式");
 * }</pre>
 *
 * @author cartethyia
 */
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
