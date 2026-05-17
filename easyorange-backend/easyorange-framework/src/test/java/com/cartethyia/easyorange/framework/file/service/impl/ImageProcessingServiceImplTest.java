package com.cartethyia.easyorange.framework.file.service.impl;

import com.cartethyia.easyorange.framework.config.properties.ImageProcessingProperties;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageProcessingServiceImplTest {

    @Mock
    private ImageProcessingProperties properties;

    private ImageProcessingServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(properties.getQuality()).thenReturn(0.80f);
        when(properties.getThumbnailQuality()).thenReturn(0.75f);
        when(properties.getResponsiveQuality()).thenReturn(0.75f);

        var pj = new ImageProcessingProperties.ProgressiveJpeg();
        pj.setEnabled(true);
        pj.setMinSize(102400);
        when(properties.getProgressiveJpeg()).thenReturn(pj);

        var sc = new ImageProcessingProperties.SmartCrop();
        sc.setEnabled(true);
        sc.setDefaultAspectRatio("1:1");
        sc.setMinEntropyThreshold(0.5);
        when(properties.getSmartCrop()).thenReturn(sc);

        service = new ImageProcessingServiceImpl(properties);
    }

    // === Quality Configuration Tests ===

    @Test
    void processImage_withExplicitQuality_shouldSucceed() throws Exception {
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        File source = tempDir.resolve("test.jpg").toFile();
        ImageIO.write(testImage, "jpg", source);

        var result = service.processImage(source, 100, 100,
                ImageProcessingService.ImageFormat.JPEG, 0.9f);

        assertNotNull(result);
        assertTrue(result.file().exists());
        assertEquals("image/jpeg", result.mimeType());
    }

    @Test
    void processImage_withDefaultQuality_shouldUseConfiguredValue() throws Exception {
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        File source = tempDir.resolve("test.jpg").toFile();
        ImageIO.write(testImage, "jpg", source);

        var result = service.processImage(source, 100, 100,
                ImageProcessingService.ImageFormat.JPEG);

        assertNotNull(result);
        verify(properties).getQuality();
    }

    @Test
    void createThumbnail_withExplicitQuality_shouldSucceed() throws Exception {
        BufferedImage testImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        File source = tempDir.resolve("test.jpg").toFile();
        ImageIO.write(testImage, "jpg", source);

        var result = service.createThumbnail(source, 100, 0.5f);

        assertNotNull(result);
        assertTrue(result.file().exists());
        assertEquals(100, ImageIO.read(result.file()).getWidth());
    }

    @Test
    void createThumbnail_withDefaultQuality_shouldUseConfiguredValue() throws Exception {
        BufferedImage testImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        File source = tempDir.resolve("test.jpg").toFile();
        ImageIO.write(testImage, "jpg", source);

        var result = service.createThumbnail(source, 100);

        assertNotNull(result);
        verify(properties).getThumbnailQuality();
    }

    @Test
    void processImage_outputFormat_shouldMatchRequested() throws Exception {
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        File source = tempDir.resolve("test.png").toFile();
        ImageIO.write(testImage, "png", source);

        var result = service.processImage(source, 100, 100,
                ImageProcessingService.ImageFormat.PNG, 0.8f);

        assertEquals("image/png", result.mimeType());
    }

    // === Image Type Checks ===

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

    // === Smart Crop Tests ===

    @Test
    void smartCrop_withLargeImage_shouldReturnTargetSize() {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 800; x++) {
            for (int y = 0; y < 600; y++) {
                image.setRGB(x, y, (x * y) % 256 | ((x + y) % 256) << 8 | ((x * x + y * y) % 256) << 16);
            }
        }

        BufferedImage cropped = service.smartCrop(image, 400, 300);

        assertNotNull(cropped);
        assertEquals(400, cropped.getWidth());
        assertEquals(300, cropped.getHeight());
    }

    @Test
    void smartCrop_withSmallImage_shouldReturnOriginal() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage cropped = service.smartCrop(image, 200, 200);

        assertNotNull(cropped);
        assertEquals(100, cropped.getWidth());
        assertEquals(100, cropped.getHeight());
    }

    @Test
    void smartCropWithFallback_whenSmallerThanTarget_shouldReturnOriginal() {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);

        BufferedImage result = service.smartCropWithFallback(image, 200, 200);

        assertSame(image, result);
    }

    @Test
    void smartCrop_withSolidColorImage_shouldNotThrow() {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);

        BufferedImage cropped = service.smartCrop(image, 100, 100);

        assertNotNull(cropped);
        assertEquals(100, cropped.getWidth());
        assertEquals(100, cropped.getHeight());
    }

    @Test
    void smartCropWithFallback_withLargeImage_shouldReturnCropped() {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 800; x++) {
            for (int y = 0; y < 800; y++) {
                image.setRGB(x, y, (x + y) % 256 | ((x - y) % 256) << 8 | (x * y % 256) << 16);
            }
        }

        BufferedImage result = service.smartCropWithFallback(image, 400, 300);

        assertNotNull(result);
        assertEquals(400, result.getWidth());
        assertEquals(300, result.getHeight());
    }
}
