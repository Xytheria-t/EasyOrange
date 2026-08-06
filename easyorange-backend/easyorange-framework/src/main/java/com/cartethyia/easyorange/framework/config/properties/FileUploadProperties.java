package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String path = "./upload";

    private String urlPrefix = "/api/file/";

    private long maxSize = CommonConstant.FILE_MAX_SIZE;

    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
}
