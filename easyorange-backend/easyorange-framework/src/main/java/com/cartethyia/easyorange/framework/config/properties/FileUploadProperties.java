package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "file.upload")
public record FileUploadProperties(
        @NotBlank @DefaultValue("./upload") String path,

        @NotBlank @DefaultValue("/api/file/") String urlPrefix,

        /** 单文件大小上限（字节），默认 {@link CommonConstant#FILE_MAX_SIZE}。 */
        @Min(1) @DefaultValue(CommonConstant.FILE_MAX_SIZE + "")
        long maxSize,

        @NotEmpty List<String> allowedExtensions) {

    public FileUploadProperties {
        allowedExtensions = allowedExtensions == null
                ? List.of("jpg", "jpeg", "png", "gif", "webp", "bmp")
                : List.copyOf(allowedExtensions);
    }
}
