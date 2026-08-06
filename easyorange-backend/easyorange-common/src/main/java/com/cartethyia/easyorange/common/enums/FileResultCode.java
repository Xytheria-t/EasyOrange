package com.cartethyia.easyorange.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件模块错误码
 * <p>
 * 错误码范围：B5001-B5999
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum FileResultCode implements IResultCode {
    FILE_UPLOAD_FAILED("B5001", "文件上传失败"),
    FILE_DELETE_FAILED("B5002", "文件删除失败"),
    FILE_NOT_FOUND("B5003", "文件不存在"),
    FILE_TYPE_NOT_ALLOWED("B5004", "文件类型不允许"),
    FILE_SIZE_EXCEEDED("B5005", "文件大小超出限制"),
    FILE_NAME_INVALID("B5006", "文件名无效");

    private final String code;
    private final String message;
}
