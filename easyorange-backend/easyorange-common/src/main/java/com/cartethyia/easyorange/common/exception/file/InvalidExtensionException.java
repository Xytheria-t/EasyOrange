package com.cartethyia.easyorange.common.exception.file;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import java.util.List;
import lombok.Getter;

@Getter
public class InvalidExtensionException extends FileException {

    private final List<String> allowedExtensions;
    private final String extension;
    private final String filename;

    public InvalidExtensionException(List<String> allowedExtensions, String extension, String filename) {
        super(FileResultCode.FILE_TYPE_NOT_ALLOWED, "文件类型不正确，允许的类型：" + String.join(", ", allowedExtensions));
        this.allowedExtensions = List.copyOf(allowedExtensions);
        this.extension = extension;
        this.filename = filename;
    }
}
