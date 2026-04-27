package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 文件扩展名不匹配异常
 * <p>
 * 当上传文件的扩展名不在允许列表中，或文件魔数与扩展名不匹配时抛出。
 * </p>
 */
@Getter
public class InvalidExtensionException extends FileException {

    private final List<String> allowedExtensions;
    private final String extension;
    private final String filename;

    public InvalidExtensionException(List<String> allowedExtensions, String extension, String filename) {
        super(FileResultCode.FILE_TYPE_NOT_ALLOWED, "文件类型不正确，允许的类型：" + String.join(", ", allowedExtensions));
        this.allowedExtensions = Collections.unmodifiableList(allowedExtensions);
        this.extension = extension;
        this.filename = filename;
    }
}
