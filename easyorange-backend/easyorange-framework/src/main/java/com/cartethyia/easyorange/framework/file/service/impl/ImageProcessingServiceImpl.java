package com.cartethyia.easyorange.framework.file.service.impl;

import com.cartethyia.easyorange.framework.config.properties.ImageProcessingProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private final ImageProcessingProperties properties;

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp");

    private static final Set<ImageFormat> SUPPORTED_OUTPUT_FORMATS =
            Set.of(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP);

    @Override
    public ProcessedImage processImage(File source, int width, int height, ImageFormat format, float quality)
            throws IOException {
        var output = createTempFile("processed_", format.extension());
        try {
            var builder = Thumbnails.of(source)
                    .size(width, height)
                    .outputQuality(quality)
                    .outputFormat(format.extension());
            if (width > 0 && height > 0) builder.crop(Positions.CENTER);
            builder.toFile(output);
            return new ProcessedImage(output, format.mimeType(), output.length());
        } catch (IOException e) {
            Files.deleteIfExists(output.toPath());
            throw e;
        }
    }

    @Override
    public ProcessedImage createThumbnail(File source, int size, float quality) throws IOException {
        var output = createTempFile("thumb_", "jpg");
        try {
            Thumbnails.of(source)
                    .size(size, size)
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toFile(output);
            return new ProcessedImage(output, "image/jpeg", output.length());
        } catch (IOException e) {
            Files.deleteIfExists(output.toPath());
            throw e;
        }
    }

    @Override
    public ImageDimensions getDimensions(Path source) throws IOException {
        try (var stream = ImageIO.createImageInputStream(Files.newInputStream(source))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("No image reader found for: " + source);
            }
            var reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                return new ImageDimensions(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public boolean isImage(String mimeType) {
        return mimeType != null && SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public boolean supportsFormat(ImageFormat format) {
        return SUPPORTED_OUTPUT_FORMATS.contains(format);
    }

    private static File createTempFile(String prefix, String suffix) throws IOException {
        var file = File.createTempFile(prefix, "." + suffix);
        file.deleteOnExit();
        return file;
    }
}
