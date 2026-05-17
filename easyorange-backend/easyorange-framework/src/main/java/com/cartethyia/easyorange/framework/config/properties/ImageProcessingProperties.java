package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "easyorange.file.image")
public class ImageProcessingProperties {

    /** Default output quality (0.0 - 1.0) */
    private float quality = 0.8f;

    /** Thumbnail output quality */
    private float thumbnailQuality = 0.75f;

    /** Responsive image output quality */
    private float responsiveQuality = 0.75f;

    /** Progressive JPEG settings */
    private ProgressiveJpeg progressiveJpeg = new ProgressiveJpeg();

    /** Smart crop settings */
    private SmartCrop smartCrop = new SmartCrop();

    @Data
    public static class ProgressiveJpeg {
        /** Enable progressive JPEG for large images */
        private boolean enabled = true;
        /** Minimum file size (bytes) to enable progressive encoding */
        private long minSize = 102400; // 100KB
    }

    @Data
    public static class SmartCrop {
        /** Enable smart cropping on upload */
        private boolean enabled = true;
        /** Default aspect ratio (e.g., "1:1", "4:3", "16:9") */
        private String defaultAspectRatio = "1:1";
        /** Minimum entropy threshold - fallback to center crop below this */
        private double minEntropyThreshold = 0.5;
    }
}
