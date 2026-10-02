package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 图片处理配置 —— 目前只有缩略图质量走配置。
 *
 * <p>默认输出质量与响应式尺寸由 {@code FileController} 的 {@code q} / {@code w} 请求参数按次决定，
 * 属于「调用方要什么就给什么」而非「服务端统一策略」；渐进式 JPEG 与智能裁剪尚未实现。
 * 这几项曾以配置项形态存在但无任何消费方，读配置的人会误以为功能已生效，故不再声明。
 */
@Validated
@ConfigurationProperties(prefix = "easyorange.file.image")
public record ImageProcessingProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.75")
        float thumbnailQuality) {}
