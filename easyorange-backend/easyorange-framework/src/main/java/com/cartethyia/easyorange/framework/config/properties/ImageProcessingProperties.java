package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 图片处理配置（上传后压缩、缩略图、响应式多尺寸、智能裁剪）。
 *
 * @param quality 默认输出质量（0.0 - 1.0）
 * @param thumbnailQuality 缩略图输出质量
 * @param responsiveQuality 响应式图片输出质量
 * @param progressiveJpeg 渐进式 JPEG 设置
 * @param smartCrop 智能裁剪设置
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.file.image")
public record ImageProcessingProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.8")
        float quality,

        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.75")
        float thumbnailQuality,

        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.75")
        float responsiveQuality,

        @Valid ProgressiveJpeg progressiveJpeg,
        @Valid SmartCrop smartCrop) {

    public ImageProcessingProperties {
        if (progressiveJpeg == null) {
            progressiveJpeg = new ProgressiveJpeg(ProgressiveJpeg.DEFAULT_ENABLED, ProgressiveJpeg.DEFAULT_MIN_SIZE);
        }
        if (smartCrop == null) {
            smartCrop = new SmartCrop(
                    SmartCrop.DEFAULT_ENABLED, SmartCrop.DEFAULT_ASPECT_RATIO, SmartCrop.DEFAULT_MIN_ENTROPY_THRESHOLD);
        }
    }

    /**
     * 渐进式 JPEG 设置。
     *
     * @param enabled 是否对大图启用渐进式编码
     * @param minSize 启用渐进式编码的最小文件大小（字节）
     */
    public record ProgressiveJpeg(
            @DefaultValue(DEFAULT_ENABLED + "") boolean enabled,
            @Min(0) @DefaultValue(DEFAULT_MIN_SIZE + "") long minSize) {

        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_MIN_SIZE = 102400L;
    }

    /**
     * 智能裁剪设置。
     *
     * @param enabled 上传时是否启用智能裁剪
     * @param defaultAspectRatio 默认宽高比（如 {@code 1:1}、{@code 4:3}、{@code 16:9}）
     * @param minEntropyThreshold 最小熵阈值，低于该值回退居中裁剪
     */
    public record SmartCrop(
            @DefaultValue(DEFAULT_ENABLED + "") boolean enabled,

            @Pattern(regexp = "\\d+:\\d+", message = "默认宽高比必须形如 16:9") @DefaultValue(DEFAULT_ASPECT_RATIO)
            String defaultAspectRatio,

            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue(DEFAULT_MIN_ENTROPY_THRESHOLD + "")
            double minEntropyThreshold) {

        private static final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_ASPECT_RATIO = "1:1";
        private static final double DEFAULT_MIN_ENTROPY_THRESHOLD = 0.5;
    }
}
