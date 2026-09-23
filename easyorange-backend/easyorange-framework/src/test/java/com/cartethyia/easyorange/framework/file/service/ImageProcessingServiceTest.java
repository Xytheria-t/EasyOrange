package com.cartethyia.easyorange.framework.file.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cartethyia.easyorange.framework.config.properties.ImageProcessingProperties;
import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageProcessingServiceTest {

    private final ImageProcessingProperties properties = PropertyBindings.bind(ImageProcessingProperties.class);

    private ImageProcessingService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new ImageProcessingService(properties);
    }

    @Test
    void processImage_withExplicitQuality_shouldSucceed() throws Exception {
        var source = createImage(200, 200, "jpg");
        var result = service.processImage(source, 100, 100, ImageProcessingService.ImageFormat.JPEG, 0.9f);
        assertNotNull(result);
        assertTrue(result.file().exists());
        assertEquals("image/jpeg", result.mimeType());
    }

    @Test
    void processImage_outputFormat_shouldMatchRequested() throws Exception {
        var source = createImage(200, 200, "png");
        var result = service.processImage(source, 100, 100, ImageProcessingService.ImageFormat.PNG, 0.8f);
        assertEquals("image/png", result.mimeType());
    }

    @Test
    void processImage_webp_degradesToJpegWhenWriterMissing() throws Exception {
        var source = createImage(200, 200, "jpg");
        var result = service.processImage(source, 100, 100, ImageProcessingService.ImageFormat.WEBP, 0.8f);

        // 无 webp 编码器的环境（JDK/Thumbnailator 默认）必须降级 JPEG 而不是抛 not supported（TD-018）
        boolean writerPresent = ImageIO.getImageWritersByFormatName("webp").hasNext();
        assertEquals(writerPresent ? "image/webp" : "image/jpeg", result.mimeType());
        assertTrue(result.file().exists());
        assertTrue(result.file().length() > 0);
    }

    @Test
    void processImage_withInvalidFile_shouldThrow() {
        assertThrows(
                Exception.class,
                () -> service.processImage(
                        new File("/nonexistent/image.jpg"), 100, 100, ImageProcessingService.ImageFormat.JPEG, 0.8f));
    }

    @Test
    void createThumbnail_shouldSucceed() throws Exception {
        var source = createImage(500, 500, "jpg");
        var result = service.createThumbnail(source, 100, 0.5f);
        assertNotNull(result);
        assertTrue(result.file().exists());
        assertEquals(100, ImageIO.read(result.file()).getWidth());
    }

    @Test
    void getDimensions_shouldReturnCorrectSize() throws Exception {
        var source = createImage(320, 240, "jpg");
        var dims = service.getDimensions(source.toPath());
        assertEquals(320, dims.width());
        assertEquals(240, dims.height());
    }

    @Test
    void isImage_withSupportedTypes_shouldReturnTrue() {
        assertTrue(service.isImage("image/jpeg"));
        assertTrue(service.isImage("image/png"));
        assertTrue(service.isImage("image/webp"));
        assertTrue(service.isImage("image/gif"));
        assertTrue(service.isImage("image/bmp"));
    }

    @Test
    void isImage_withUnsupportedType_shouldReturnFalse() {
        assertFalse(service.isImage("application/pdf"));
        assertFalse(service.isImage(null));
        assertFalse(service.isImage(""));
    }

    @Test
    void supportsFormat_forSupportedFormats_shouldReturnTrue() {
        assertTrue(service.supportsFormat(ImageProcessingService.ImageFormat.JPEG));
        assertTrue(service.supportsFormat(ImageProcessingService.ImageFormat.PNG));
        assertTrue(service.supportsFormat(ImageProcessingService.ImageFormat.WEBP));
    }

    @Test
    void supportsFormat_forAVIF_shouldReturnFalse() {
        assertFalse(service.supportsFormat(ImageProcessingService.ImageFormat.AVIF));
    }

    private File createImage(int w, int h, String format) throws Exception {
        var file = tempDir.resolve("test_" + w + "x" + h + "." + format).toFile();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), format, file);
        return file;
    }
}
