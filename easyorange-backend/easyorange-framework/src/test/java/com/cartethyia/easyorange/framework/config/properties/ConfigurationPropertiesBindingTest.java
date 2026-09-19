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
import org.springframework.boot.web.server.Cookie;
import org.springframework.context.annotation.Configuration;

/**
 * 框架层配置属性绑定守卫 — 空环境下所有 {@code @ConfigurationProperties} 必须构造器绑定成功。
 * <p>
 * 覆盖 record 构造器绑定的四类易错点：{@code @DefaultValue} 默认值、集合/嵌套对象缺省时的
 * 空值兜底（紧凑构造器）、{@code @Validated} 约束在启动期生效、字符串到枚举的宽松绑定。
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
    @DisplayName("标量缺省值来自 @DefaultValue")
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
                            .smartCrop()
                            .defaultAspectRatio())
                    .isEqualTo("1:1");
            assertThat(context.getBean(SlowSqlProperties.class).logLevel()).isEqualTo(SlowSqlProperties.LogLevel.WARN);
            assertThat(context.getBean(MybatisPlusInterceptorProperties.class).maxLimit())
                    .isEqualTo(100L);
        });
    }

    @Test
    @DisplayName("嵌套配置组整体缺省时，回退到与字段级 @DefaultValue 同一组默认值")
    void nestedGroupsFallBackToFieldDefaults() {
        runner.run(context -> {
            var image = context.getBean(CacheProperties.class).image();
            assertThat(image.maxSize()).isEqualTo(1000);
            assertThat(image.expireHours()).isEqualTo(24);

            var jpeg = context.getBean(ImageProcessingProperties.class).progressiveJpeg();
            assertThat(jpeg.enabled()).isTrue();
            assertThat(jpeg.minSize()).isEqualTo(102400L);
            assertThat(context.getBean(ImageProcessingProperties.class)
                            .smartCrop()
                            .minEntropyThreshold())
                    .isEqualTo(0.5);

            var repeatSubmit = context.getBean(RateLimitFilterProperties.class).repeatSubmit();
            assertThat(repeatSubmit.enabled()).isTrue();
            assertThat(repeatSubmit.intervalMs()).isEqualTo(3000L);
            assertThat(repeatSubmit.message()).isEqualTo("不允许重复提交");
            assertThat(repeatSubmit.methods()).isEmpty();
            assertThat(repeatSubmit.excludePathPatterns()).isEmpty();
        });
    }

    @Test
    @DisplayName("字符串到枚举按宽松规则绑定（yml 里的小写值映射到枚举常量）")
    void bindsEnumValuesLeniently() {
        runner.withPropertyValues(
                        "slow-sql.log-level=debug",
                        "rate-limit-filter.rules[0].path-pattern=/api/**",
                        "rate-limit-filter.rules[0].strategy=local",
                        "jwt.refresh-cookie-same-site=strict")
                .run(context -> {
                    assertThat(context.getBean(SlowSqlProperties.class).logLevel())
                            .isEqualTo(SlowSqlProperties.LogLevel.DEBUG);
                    assertThat(context.getBean(RateLimitFilterProperties.class)
                                    .rules()
                                    .getFirst()
                                    .strategy())
                            .isEqualTo(RateLimitFilterProperties.Strategy.LOCAL);
                    assertThat(context.getBean(JwtProperties.class).refreshCookieSameSite())
                            .isEqualTo(Cookie.SameSite.STRICT);
                });
    }

    @Test
    @DisplayName("项目前缀生效：easyorange.mybatis-plus 与 MyBatis-Plus 自身的 mybatis-plus 前缀各绑各的")
    void bindsProjectScopedPrefix() {
        runner.withPropertyValues("easyorange.mybatis-plus.max-limit=50")
                .run(context -> assertThat(context.getBean(MybatisPlusInterceptorProperties.class)
                                .maxLimit())
                        .isEqualTo(50L));
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

        @Test
        @DisplayName("枚举取值非法启动即失败，不再静默退化")
        void rejectsInvalidEnumValue() {
            runner.withPropertyValues("slow-sql.log-level=wran")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("宽高比格式非法启动即失败（@Pattern 生效）")
        void rejectsMalformedAspectRatio() {
            runner.withPropertyValues("easyorange.file.image.smart-crop.default-aspect-ratio=1x1")
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
