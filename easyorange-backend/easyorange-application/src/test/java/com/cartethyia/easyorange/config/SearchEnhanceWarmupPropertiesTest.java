package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("搜索增强预热配置绑定")
class SearchEnhanceWarmupPropertiesTest {

    /** 直接读真实 application-dev.yaml：YAML 列表绑不进 @Value，只能靠 properties 类接住 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                try {
                    for (PropertySource<?> source : new YamlPropertySourceLoader()
                            .load("application-dev", new ClassPathResource("application-dev.yaml"))) {
                        context.getEnvironment().getPropertySources().addFirst(source);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            })
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("dev：开关开、查询串为列表、刷新间隔 4 分钟")
    void bindsDevYaml() {
        runner.run(context -> {
            var properties = context.getBean(SearchEnhanceWarmupProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.queries()).containsExactly("适合拍夜景的相机");
            assertThat(properties.fixedDelayMs()).isEqualTo(240_000L);
        });
    }

    @Test
    @DisplayName("未配置：默认关闭且无查询串（prod 不显式开就不会预热）")
    void defaultsDisabled() {
        new ApplicationContextRunner().withUserConfiguration(TestConfig.class).run(context -> {
            var properties = context.getBean(SearchEnhanceWarmupProperties.class);
            assertThat(properties.enabled()).isFalse();
            assertThat(properties.queries()).isEmpty();
            assertThat(properties.fixedDelayMs()).isEqualTo(240_000L);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SearchEnhanceWarmupProperties.class)
    static class TestConfig {}
}
