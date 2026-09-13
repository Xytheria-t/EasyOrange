package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "easyorange.file.image")
public record ImageProcessingProperties(
        /** Default output quality (0.0 - 1.0) */
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.8")
        float quality,

        /** Thumbnail output quality */
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.75")
        float thumbnailQuality,

        /** Responsive image output quality */
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.75")
        float responsiveQuality,

        /** Progressive JPEG settings */
        @Valid ProgressiveJpeg progressiveJpeg,

        /** Smart crop settings */
        @Valid SmartCrop smartCrop) {

    public ImageProcessingProperties {
        if (progressiveJpeg == null) {
            progressiveJpeg = new ProgressiveJpeg(true, 102400L);
        }
        if (smartCrop == null) {
            smartCrop = new SmartCrop(true, "1:1", 0.5);
        }
    }

    public record ProgressiveJpeg(
            /** Enable progressive JPEG for large images */
            @DefaultValue("true") boolean enabled,

            /** Minimum file size (bytes) to enable progressive encoding */
            @Min(0) @DefaultValue("102400") long minSize) {}

    public record SmartCrop(
            /** Enable smart cropping on upload */
            @DefaultValue("true") boolean enabled,

            /** Default aspect ratio (e.g., "1:1", "4:3", "16:9") */
            @DefaultValue("1:1") String defaultAspectRatio,

            /** Minimum entropy threshold - fallback to center crop below this */
            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.5")
            double minEntropyThreshold) {}
}
