package com.cartethyia.easyorange.framework.config.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "webmvc")
public class WebMvcProperties {

    private List<String> excludePaths = new ArrayList<>();

    private List<String> skipLoggingPaths = new ArrayList<>();

    private int interceptorOrder = 0;

    public List<String> getExcludePaths() {
        return List.copyOf(excludePaths);
    }

    public List<String> getSkipLoggingPaths() {
        return List.copyOf(skipLoggingPaths);
    }
}
