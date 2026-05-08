package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import com.cartethyia.easyorange.common.util.FileSizeFormat;
import lombok.Getter;

/**
 * 文件大小超出限制异常
 * <p>
 * 当上传文件的大小超过系统允许的最大值时抛出。
 * </p>
 */
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
