package com.cartethyia.easyorange.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件模块错误码
 * <p>
 * 错误码范围：B5001-B5999（B5002/B5006 已删除：无真实语义站点，文件删除失败仅记录日志、
 * 无文件名校验逻辑）。HTTP 状态映射见 {@link IResultCode#resolveStatus(String)}。
 * </p>
 *
 * @author cartethyia
 * @see IResultCode
 */
@Getter
@AllArgsConstructor
public enum FileResultCode implements IResultCode {
    FILE_UPLOAD_FAILED("B5001", "文件上传失败"),
    FILE_NOT_FOUND("B5003", "文件不存在"),
    FILE_TYPE_NOT_ALLOWED("B5004", "文件类型不允许"),
    FILE_SIZE_EXCEEDED("B5005", "文件大小超出限制");

    private final String code;
    private final String message;
}
