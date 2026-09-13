package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 文件上传配置。
 *
 * @param path 上传文件落盘根目录
 * @param urlPrefix 访问上传文件的 URL 前缀
 * @param maxSize 单文件大小上限（字节），默认 {@link CommonConstant#FILE_MAX_SIZE}
 * @param allowedExtensions 允许的扩展名（小写、不含点）；显式配成空列表会被 {@code @NotEmpty} 拒绝
 */
@Validated
@ConfigurationProperties(prefix = "file.upload")
public record FileUploadProperties(
        @NotBlank @DefaultValue("./upload") String path,
        @NotBlank @DefaultValue("/api/file/") String urlPrefix,

        @Min(1) @DefaultValue(CommonConstant.FILE_MAX_SIZE + "")
        long maxSize,

        @NotEmpty List<String> allowedExtensions) {

    public FileUploadProperties {
        allowedExtensions = allowedExtensions == null
                ? List.of("jpg", "jpeg", "png", "gif", "webp", "bmp")
                : List.copyOf(allowedExtensions);
    }
}
