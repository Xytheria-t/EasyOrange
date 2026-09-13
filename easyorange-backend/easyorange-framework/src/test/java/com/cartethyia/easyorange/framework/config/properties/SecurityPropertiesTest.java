package com.cartethyia.easyorange.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityProperties Tests")
class SecurityPropertiesTest {

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
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("路径列表缺省时收敛为空列表，不再抛 null 异常")
        void validate_withMissingPaths_shouldPassWithEmptyLists() {
            var properties = new SecurityProperties(null, null, null, null, null, "/api/auth/logout", 10);

            properties.validate();

            assertThat(properties.ignorePaths()).isEmpty();
            assertThat(properties.productPaths()).isEmpty();
            assertThat(properties.staticPaths()).isEmpty();
            assertThat(properties.allowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("should throw when password encoder strength is below 4")
        void validate_withLowPasswordStrength_shouldThrow() {
            var properties = PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "3");

            assertThatThrownBy(properties::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("密码加密强度必须在 4-31 之间");
        }

        @Test
        @DisplayName("should throw when password encoder strength is above 31")
        void validate_withHighPasswordStrength_shouldThrow() {
            var properties = PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "32");

            assertThatThrownBy(properties::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("密码加密强度必须在 4-31 之间");
        }

        @Test
        @DisplayName("should pass with valid configuration")
        void validate_withValidConfig_shouldPass() {
            var properties = new SecurityProperties(
                    List.of("/api/public/**"),
                    List.of("/api/products/**"),
                    List.of("/static/**"),
                    List.of("https://example.com"),
                    null,
                    "/api/auth/logout",
                    12);

            // Should not throw
            properties.validate();
        }

        @Test
        @DisplayName("should warn but not throw for CORS wildcard")
        void validate_withAllowedOriginsWildcard_shouldNotThrow() {
            var properties = new SecurityProperties(null, null, null, List.of("*"), null, "/api/auth/logout", 10);

            // Should not throw, only logs warning
            properties.validate();
        }

        @Test
        @DisplayName("should warn but not throw for low password strength")
        void validate_withLowStrengthWarning_shouldNotThrow() {
            var properties = PropertyBindings.bind(SecurityProperties.class, "password-encoder-strength", "8");

            // Should not throw (only logs warning)
            properties.validate();
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
