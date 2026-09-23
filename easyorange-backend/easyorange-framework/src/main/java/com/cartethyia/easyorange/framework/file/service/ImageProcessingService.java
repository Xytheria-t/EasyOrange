package com.cartethyia.easyorange.framework.file.service;

import com.cartethyia.easyorange.framework.config.properties.ImageProcessingProperties;
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
public class ImageProcessingService {

    public record ImageDimensions(int width, int height) {}

    public enum ImageFormat {
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

        public String extension() {
            return extension;
        }

        public String mimeType() {
            return mimeType;
        }
    }

    public record ProcessedImage(File file, String mimeType, long size) {}

    private final ImageProcessingProperties properties;

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp");

    private static final Set<ImageFormat> SUPPORTED_OUTPUT_FORMATS =
            Set.of(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP);

    /**
     * 运行时探测 webp 编码器（TD-018）：JDK / Thumbnailator 默认不带 webp writer，
     * 请求 webp 输出会抛 {@code Specified format is not supported: webp} → by-id view/responsive 500。
     * 启动期探测一次；无 writer 时 webp 请求降级为 JPEG（对齐扩展名与 Content-Type）。
     */
    private static final boolean WEBP_WRITER_PRESENT = detectWebpWriter();

    private static boolean detectWebpWriter() {
        try {
            return ImageIO.getImageWritersByFormatName("webp").hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    public ProcessedImage processImage(File source, int width, int height, ImageFormat format, float quality)
            throws IOException {
        if (format == ImageFormat.WEBP && !WEBP_WRITER_PRESENT) {
            log.debug("webp writer unavailable, degrade output to jpeg");
            format = ImageFormat.JPEG;
        }
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

    public boolean isImage(String mimeType) {
        return mimeType != null && SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }

    public boolean supportsFormat(ImageFormat format) {
        return SUPPORTED_OUTPUT_FORMATS.contains(format);
    }

    private static File createTempFile(String prefix, String suffix) throws IOException {
        var file = File.createTempFile(prefix, "." + suffix);
        file.deleteOnExit();
        return file;
    }
}
