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
    public ProcessedImage processImage(File source, int width, int height, ImageFormat format) throws IOException {
        return processImage(source, width, height, format, 0.8f);
    }

    @Override
    public ProcessedImage createThumbnail(File source, int size, float quality) throws IOException {
        File outputFile = File.createTempFile("thumb_", ".jpg");

        Thumbnails.of(source)
                .size(size, size)
                .outputQuality(quality)
                .outputFormat("jpg")
                .toFile(outputFile);

        log.debug("Created thumbnail: {} -> {} ({}x{}, quality={})",
                source.getName(), outputFile.getName(), size, size, quality);

        return new ProcessedImage(outputFile, "image/jpeg", outputFile.length());
    }

    @Override
    public BufferedImage smartCrop(BufferedImage source, int targetWidth, int targetHeight) {
        int width = source.getWidth();
        int height = source.getHeight();

        int cropWidth = Math.min(targetWidth, width);
        int cropHeight = Math.min(targetHeight, height);

        int gridCols = Math.max(4, width / 50);
        int gridRows = Math.max(4, height / 50);
        int cellW = width / gridCols;
        int cellH = height / gridRows;

        float[][] entropies = new float[gridRows][gridCols];
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                entropies[r][c] = calculateEntropy(source, c * cellW, r * cellH, cellW, cellH);
            }
        }

        int windowCols = Math.max(1, cropWidth / cellW);
        int windowRows = Math.max(1, cropHeight / cellH);

        int bestR = 0;
        int bestC = 0;
        float bestScore = -1;

        for (int r = 0; r <= gridRows - windowRows; r++) {
            for (int c = 0; c <= gridCols - windowCols; c++) {
                float score = 0;
                for (int dr = 0; dr < windowRows; dr++) {
                    for (int dc = 0; dc < windowCols; dc++) {
                        score += entropies[r + dr][c + dc];
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestR = r;
                    bestC = c;
                }
            }
        }

        int x = Math.min(bestC * cellW, width - cropWidth);
        int y = Math.min(bestR * cellH, height - cropHeight);

        return source.getSubimage(x, y, cropWidth, cropHeight);
    }

    @Override
    public BufferedImage smartCropWithFallback(BufferedImage source, int targetWidth, int targetHeight) {
        if (source.getWidth() < targetWidth || source.getHeight() < targetHeight) {
            return source;
        }
        return smartCrop(source, targetWidth, targetHeight);
    }

    private float calculateEntropy(BufferedImage image, int x, int y, int w, int h) {
        int[] histogram = new int[256];
        int total = w * h;

        for (int py = y; py < y + h && py < image.getHeight(); py++) {
            for (int px = x; px < x + w && px < image.getWidth(); px++) {
                int rgb = image.getRGB(px, py);
                int gray = ((rgb >> 16) & 0xFF) * 77 + ((rgb >> 8) & 0xFF) * 151 + (rgb & 0xFF) * 28 >> 8;
                histogram[Math.min(gray, 255)]++;
            }
        }

        float entropy = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                float p = (float) histogram[i] / total;
                entropy -= p * (float) (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
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
