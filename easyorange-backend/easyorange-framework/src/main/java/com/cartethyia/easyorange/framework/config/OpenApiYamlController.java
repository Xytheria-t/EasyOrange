package com.cartethyia.easyorange.framework.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serves the static OpenAPI YAML definition for Swagger UI.
 */
@RestController
@RequestMapping("/openapi")
public class OpenApiYamlController {

    private static final String OPENAPI_YAML = "openapi.yaml";

    @GetMapping(value = "/spec", produces = "application/yaml")
    public String openApiSpec() throws IOException {
        ClassPathResource resource = new ClassPathResource(OPENAPI_YAML);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
