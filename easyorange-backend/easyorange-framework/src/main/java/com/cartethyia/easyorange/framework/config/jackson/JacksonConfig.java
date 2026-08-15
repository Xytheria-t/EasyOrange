package com.cartethyia.easyorange.framework.config.jackson;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * JSON 序列化配置 — Long → String（前端 JS 精度安全）。
 * <p>
 * 只需提供 {@code JsonMapper} bean：Boot 的 {@code JacksonAutoConfiguration}
 * 带 {@code @ConditionalOnMissingBean}，检测到用户 mapper 后整体退避，
 * 全局 ObjectMapper 均继承此约定，无需再注册 {@code JsonMapperBuilderCustomizer}。
 */
@AutoConfiguration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().addModule(longToStringModule()).build();
    }

    private SimpleModule longToStringModule() {
        return new SimpleModule()
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
    }
}
