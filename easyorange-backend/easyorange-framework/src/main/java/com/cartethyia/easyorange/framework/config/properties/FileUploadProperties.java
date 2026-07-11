package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String path = "./upload";

    private String urlPrefix = "/api/file/";

    private long maxSize = CommonConstant.FILE_MAX_SIZE;

    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
}