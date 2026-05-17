package com.cartethyia.easyorange.framework.file.service;

import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

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

    /** Process image with specified quality */
    ProcessedImage processImage(File source, int width, int height, ImageFormat format, float quality) throws IOException;

    /** Process image with default quality from configuration */
    ProcessedImage processImage(File source, int width, int height, ImageFormat format) throws IOException;

    /** Create thumbnail with specified quality */
    ProcessedImage createThumbnail(File source, int size, float quality) throws IOException;

    /** Create thumbnail with default quality from configuration */
    ProcessedImage createThumbnail(File source, int size) throws IOException;

    /** Smart crop: identify the most information-rich region and crop to target dimensions */
    BufferedImage smartCrop(BufferedImage source, int targetWidth, int targetHeight);

    /** Smart crop with fallback: returns original if source is smaller than target */
    BufferedImage smartCropWithFallback(BufferedImage source, int targetWidth, int targetHeight);

    boolean isImage(String mimeType);

    boolean supportsFormat(ImageFormat format);
}
