package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "webmvc")
public class WebMvcProperties {

    private List<String> excludePaths = new ArrayList<>();

    private List<String> skipLoggingPaths = new ArrayList<>();

    private int interceptorOrder = 0;

    public List<String> getExcludePaths() {
        return Collections.unmodifiableList(excludePaths);
    }

    public List<String> getSkipLoggingPaths() {
        return Collections.unmodifiableList(skipLoggingPaths);
    }
}