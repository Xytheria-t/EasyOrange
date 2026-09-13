package com.cartethyia.easyorange.framework.testsupport;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 测试用配置绑定夹具 — 经真实 {@link Binder} 构造实例，使 record 的默认值
 * （{@code @DefaultValue} / 紧凑构造器）保持单一来源，测试不重复维护一份默认值副本。
 * <p>
 * 覆盖项按「相对本类 prefix 的 kebab-case key」书写：
 * <pre>{@code
 * PropertyBindings.bind(JwtProperties.class)                        // 全默认值
 * PropertyBindings.bind(JwtProperties.class, "issuer", "x")         // jwt.issuer=x
 * }</pre>
 */
public final class PropertyBindings {

    private PropertyBindings() {}

    /** 按相对键值对绑定；不传覆盖项时返回全默认值实例。 */
    public static <T> T bind(Class<T> type, String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("覆盖项必须成对出现: " + Arrays.toString(keyValues));
        }
        String prefix = prefixOf(type);
        Map<String, String> overrides = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            overrides.put(prefix + "." + keyValues[i], keyValues[i + 1]);
        }
        return new Binder(new MapConfigurationPropertySource(overrides)).bindOrCreate(prefix, type);
    }

    private static String prefixOf(Class<?> type) {
        var annotation = type.getAnnotation(ConfigurationProperties.class);
        if (annotation == null || annotation.prefix().isEmpty()) {
            throw new IllegalArgumentException(type.getName() + " 缺少 @ConfigurationProperties(prefix)");
        }
        return annotation.prefix();
    }
}
