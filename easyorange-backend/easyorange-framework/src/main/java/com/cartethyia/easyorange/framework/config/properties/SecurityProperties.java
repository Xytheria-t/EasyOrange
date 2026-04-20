package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> ignorePaths = List.of();
    private List<String> productPaths = List.of();
    private List<String> staticPaths = List.of();
    private List<String> allowedOrigins = List.of();
    private String logoutUrl = "/api/auth/logout";
}
