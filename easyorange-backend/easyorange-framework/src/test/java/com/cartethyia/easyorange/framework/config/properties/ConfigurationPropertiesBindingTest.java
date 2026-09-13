package com.cartethyia.easyorange.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 框架层配置属性绑定守卫 — 空环境下所有 {@code @ConfigurationProperties} 必须构造器绑定成功。
 * <p>
 * 覆盖 record 构造器绑定的三类易错点：{@code @DefaultValue} 默认值、集合/嵌套对象缺省时的
 * 空值兜底（紧凑构造器）、{@code @Validated} 约束在启动期生效。
 */
@DisplayName("ConfigurationProperties 构造器绑定")
class ConfigurationPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesScanConfig.class);

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan
    static class PropertiesScanConfig {}

    @Test
    @DisplayName("空属性源下全部 Properties 均可绑定（缺省值兜底，不抛 BindException）")
    void bindsAllPropertiesFromEmptyEnvironment() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("缺省值来自 @DefaultValue 与紧凑构造器")
    void appliesDefaults() {
        runner.run(context -> {
            assertThat(context.getBean(LockProperties.class).holdWarnThreshold())
                    .isEqualTo(Duration.ofSeconds(60));
            assertThat(context.getBean(WebMvcProperties.class).excludePaths()).isEmpty();
            assertThat(context.getBean(WebMvcProperties.class).interceptorOrder())
                    .isZero();
            assertThat(context.getBean(ImageProcessingProperties.class).quality())
                    .isEqualTo(0.8f);
            assertThat(context.getBean(ImageProcessingProperties.class)
                            .progressiveJpeg()
                            .enabled())
                    .isTrue();
            assertThat(context.getBean(ImageProcessingProperties.class)
                            .smartCrop()
                            .defaultAspectRatio())
                    .isEqualTo("1:1");
        });
    }

    @Nested
    @DisplayName("约束校验")
    class ValidationTests {

        @Test
        @DisplayName("越界值启动即失败（@Validated 生效）")
        void rejectsOutOfRangeQuality() {
            runner.withPropertyValues("easyorange.file.image.quality=1.5")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("嵌套对象约束级联生效（@Valid 生效）")
        void rejectsInvalidNestedValue() {
            runner.withPropertyValues("easyorange.file.image.smart-crop.min-entropy-threshold=2.0")
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
