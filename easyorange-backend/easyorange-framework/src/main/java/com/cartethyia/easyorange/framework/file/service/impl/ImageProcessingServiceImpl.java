package com.cartethyia.easyorange.framework.file.service.impl;

import com.cartethyia.easyorange.framework.file.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Set<ImageFormat> SUPPORTED_OUTPUT_FORMATS = Set.of(
            ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP
    );

    @Override
    public ProcessedImage processImage(File source, int width, int height, ImageFormat format, float quality) throws IOException {
        String outputExtension = format.getExtension();
        File outputFile = File.createTempFile("processed_", "." + outputExtension);

        Thumbnails.Builder<File> builder = Thumbnails.of(source)
                .size(width, height)
                .outputQuality(quality)
                .outputFormat(outputExtension);

        if (width > 0 && height > 0) {
            builder.crop(Positions.CENTER);
        }

        builder.toFile(outputFile);

        log.debug("Processed image: {} -> {} ({}x{}, quality={}, format={})",
                source.getName(), outputFile.getName(), width, height, quality, format);

        return new ProcessedImage(outputFile, format.getMimeType(), outputFile.length());
    }

    @Override
    public ProcessedImage createThumbnail(File source, int size) throws IOException {
        File outputFile = File.createTempFile("thumb_", ".jpg");

        Thumbnails.of(source)
                .size(size, size)
                .outputQuality(0.8f)
                .outputFormat("jpg")
                .toFile(outputFile);

        log.debug("Created thumbnail: {} -> {} ({}x{})",
                source.getName(), outputFile.getName(), size, size);

        return new ProcessedImage(outputFile, "image/jpeg", outputFile.length());
    }

    @Override
    public boolean isImage(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public boolean supportsFormat(ImageFormat format) {
        return SUPPORTED_OUTPUT_FORMATS.contains(format);
    }
}
