package com.cartethyia.easyorange.framework.config.async;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@AutoConfiguration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder.addModule(longToStringModule());
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .addModule(longToStringModule())
                .build();
    }

    private SimpleModule longToStringModule() {
        return new SimpleModule()
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
    }
}
