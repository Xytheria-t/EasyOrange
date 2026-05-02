package com.cartethyia.easyorange.framework.config.async;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
