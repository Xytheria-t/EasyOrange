package com.cartethyia.easyorange.framework.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "webmvc")
public record WebMvcProperties(
        List<String> excludePaths,
        List<String> skipLoggingPaths,
        @DefaultValue("0") int interceptorOrder) {

    public WebMvcProperties {
        excludePaths = excludePaths == null ? List.of() : List.copyOf(excludePaths);
        skipLoggingPaths = skipLoggingPaths == null ? List.of() : List.copyOf(skipLoggingPaths);
    }
}
