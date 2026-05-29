package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String path = "./upload";

    private String urlPrefix = "/api/file/";

    private long maxSize = 10 * 1024 * 1024;

    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
}