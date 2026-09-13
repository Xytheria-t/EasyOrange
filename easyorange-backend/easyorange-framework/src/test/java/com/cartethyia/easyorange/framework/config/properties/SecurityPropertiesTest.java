package com.cartethyia.easyorange.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("SecurityProperties Tests")
class SecurityPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<ConstraintViolation<SecurityProperties>> violations(SecurityProperties properties) {
        return validator.validate(properties);
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        /** 空属性源绑定结果 —— 默认值来自 @DefaultValue / 紧凑构造器。 */
        private final SecurityProperties properties = PropertyBindings.bind(SecurityProperties.class);

        @Test
        @DisplayName("should have empty ignorePaths by default")
        void ignorePaths_default_shouldBeEmpty() {
            assertThat(properties.ignorePaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty productPaths by default")
        void productPaths_default_shouldBeEmpty() {
            assertThat(properties.productPaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty staticPaths by default")
        void staticPaths_default_shouldBeEmpty() {
            assertThat(properties.staticPaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty allowedOrigins by default")
        void allowedOrigins_default_shouldBeEmpty() {
            assertThat(properties.allowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("should protect refresh and logout with a custom header by default")
        void csrfProtectedPaths_default_shouldCoverRefreshAndLogout() {
            assertThat(properties.csrfProtectedPaths()).containsExactly("/api/auth/refresh", "/api/auth/logout");
        }

        @Test
        @DisplayName("should have default logout URL")
        void logoutUrl_default_shouldBeApiAuthLogout() {
            assertThat(properties.logoutUrl()).isEqualTo("/api/auth/logout");
        }

        @Test
        @DisplayName("should have default password encoder strength of 10")
        void passwordEncoderStrength_default_shouldBe10() {
            assertThat(properties.passwordEncoderStrength()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("路径列表缺省")
    class NullPathNormalizationTests {

        @Test
        @DisplayName("路径列表缺省时收敛为空列表，不再抛 null 异常")
        void missingPaths_shouldConvergeToEmptyLists() {
            var properties = new SecurityProperties(null, null, null, null, null, "/api/auth/logout", 10);

            assertThat(properties.ignorePaths()).isEmpty();
            assertThat(properties.productPaths()).isEmpty();
            assertThat(properties.staticPaths()).isEmpty();
            assertThat(properties.allowedOrigins()).isEmpty();
        }
    }

    @Nested
    @DisplayName("密码强度约束")
    class PasswordEncoderStrengthTests {

        @Test
        @DisplayName("低于 4 违反约束")
        void belowMin_shouldHaveViolation() {
            var properties = PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "3");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("密码加密强度必须在 4-31 之间"));
        }

        @Test
        @DisplayName("高于 31 违反约束")
        void aboveMax_shouldHaveViolation() {
            var properties = PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "32");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("密码加密强度必须在 4-31 之间"));
        }

        @Test
        @DisplayName("边界值 4 与 31 合法")
        void bounds_shouldHaveNoViolations() {
            assertThat(violations(PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "4")))
                    .isEmpty();
            assertThat(violations(PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "31")))
                    .isEmpty();
        }

        @Test
        @DisplayName("偏离推荐区间（8 或 15）只是告警，不是约束违规")
        void offRecommendedRange_shouldHaveNoViolations() {
            assertThat(violations(PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "8")))
                    .isEmpty();
            assertThat(violations(PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "15")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("启动期行为")
    class StartupTests {

        private final ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
                .withUserConfiguration(SecurityPropertiesOnlyConfig.class);

        @Configuration(proxyBeanMethods = false)
        @EnableConfigurationProperties(SecurityProperties.class)
        static class SecurityPropertiesOnlyConfig {}

        @Test
        @DisplayName("越界强度由约束在绑定期拦下，启动即失败")
        void outOfRangeStrength_shouldFailStartup() {
            runner.withPropertyValues("security.password-encoder-strength=3")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("CORS 通配只告警不拦启动（@PostConstruct 告警确实执行）")
        void wildcardOrigins_shouldWarnWithoutFailingStartup(CapturedOutput output) {
            runner.withPropertyValues("security.allowed-origins[0]=*", "security.password-encoder-strength=8")
                    .run(context -> assertThat(context).hasNotFailed());

            assertThat(output).contains("CORS 允许所有源").doesNotContain("密码加密强度必须在 4-31 之间");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("ignorePaths should return unmodifiable copy")
        void ignorePaths_shouldBeUnmodifiable() {
            var source = new ArrayList<>(List.of("/api/public"));
            var properties = new SecurityProperties(source, null, null, null, null, "/api/auth/logout", 10);

            List<String> paths = properties.ignorePaths();
            source.add("/api/other");

            assertThatThrownBy(() -> paths.add("/api/other")).isInstanceOf(UnsupportedOperationException.class);
            assertThat(paths).containsExactly("/api/public");
        }

        @Test
        @DisplayName("productPaths should return unmodifiable copy")
        void productPaths_shouldBeUnmodifiable() {
            var source = new ArrayList<>(List.of("/api/products/**"));
            var properties = new SecurityProperties(null, source, null, null, null, "/api/auth/logout", 10);

            List<String> paths = properties.productPaths();
            source.add("/api/other");

            assertThatThrownBy(() -> paths.add("/api/other")).isInstanceOf(UnsupportedOperationException.class);
            assertThat(paths).containsExactly("/api/products/**");
        }

        @Test
        @DisplayName("staticPaths should return unmodifiable copy")
        void staticPaths_shouldBeUnmodifiable() {
            var source = new ArrayList<>(List.of("/static/**"));
            var properties = new SecurityProperties(null, null, source, null, null, "/api/auth/logout", 10);

            List<String> paths = properties.staticPaths();
            source.add("/api/other");

            assertThatThrownBy(() -> paths.add("/api/other")).isInstanceOf(UnsupportedOperationException.class);
            assertThat(paths).containsExactly("/static/**");
        }

        @Test
        @DisplayName("allowedOrigins should return unmodifiable copy")
        void allowedOrigins_shouldBeUnmodifiable() {
            var source = new ArrayList<>(List.of("https://example.com"));
            var properties = new SecurityProperties(null, null, null, source, null, "/api/auth/logout", 10);

            List<String> origins = properties.allowedOrigins();
            source.add("https://other.com");

            assertThatThrownBy(() -> origins.add("https://other.com"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(origins).containsExactly("https://example.com");
        }
    }
}
