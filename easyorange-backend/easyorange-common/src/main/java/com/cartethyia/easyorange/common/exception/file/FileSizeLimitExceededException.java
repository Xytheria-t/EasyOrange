package com.cartethyia.easyorange.common.exception.file;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import com.cartethyia.easyorange.common.util.FileSizeFormat;
import lombok.Getter;

@Getter
public class FileSizeLimitExceededException extends FileException {

    private final long maxSizeBytes;
    private final long actualSizeBytes;

    public FileSizeLimitExceededException(long maxSizeBytes, long actualSizeBytes) {
        super(FileResultCode.FILE_SIZE_EXCEEDED,
                "文件大小超过限制：最大 " + FileSizeFormat.formatFileSize(maxSizeBytes)
                        + "，当前 " + FileSizeFormat.formatFileSize(actualSizeBytes));
        this.maxSizeBytes = maxSizeBytes;
        this.actualSizeBytes = actualSizeBytes;
    }
}