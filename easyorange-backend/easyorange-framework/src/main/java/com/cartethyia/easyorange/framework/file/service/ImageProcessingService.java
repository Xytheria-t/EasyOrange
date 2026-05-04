package com.cartethyia.easyorange.framework.file.service;

import java.io.File;
import java.io.IOException;

public interface ImageProcessingService {

    enum ImageFormat {
        WEBP("webp", "image/webp"),
        AVIF("avif", "image/avif"),
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png");

        private final String extension;
        private final String mimeType;

        ImageFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    record ProcessedImage(File file, String mimeType, long size) {}

    ProcessedImage processImage(File source, int width, int height, ImageFormat format, float quality) throws IOException;

    ProcessedImage createThumbnail(File source, int size) throws IOException;

    boolean isImage(String mimeType);

    boolean supportsFormat(ImageFormat format);
}
