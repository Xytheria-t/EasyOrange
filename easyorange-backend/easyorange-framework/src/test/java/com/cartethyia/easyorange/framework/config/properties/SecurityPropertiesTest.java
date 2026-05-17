package com.cartethyia.easyorange.framework.config.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityProperties Tests")
class SecurityPropertiesTest {

    private SecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have empty ignorePaths by default")
        void ignorePaths_default_shouldBeEmpty() {
            assertThat(properties.getIgnorePaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty productPaths by default")
        void productPaths_default_shouldBeEmpty() {
            assertThat(properties.getProductPaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty staticPaths by default")
        void staticPaths_default_shouldBeEmpty() {
            assertThat(properties.getStaticPaths()).isEmpty();
        }

        @Test
        @DisplayName("should have empty allowedOrigins by default")
        void allowedOrigins_default_shouldBeEmpty() {
            assertThat(properties.getAllowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("should have default logout URL")
        void logoutUrl_default_shouldBeApiAuthLogout() {
            assertThat(properties.getLogoutUrl()).isEqualTo("/api/auth/logout");
        }

        @Test
        @DisplayName("should have default password encoder strength of 10")
        void passwordEncoderStrength_default_shouldBe10() {
            assertThat(properties.getPasswordEncoderStrength()).isEqualTo(10);
        }

        @Test
        @DisplayName("should have xssProtectionEnabled as false by default")
        void xssProtectionEnabled_default_shouldBeFalse() {
            assertThat(properties.isXssProtectionEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should throw when ignorePaths is null")
        void validate_withNullIgnorePaths_shouldThrow() {
            properties.setIgnorePaths(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ignorePaths")
                    .hasMessageContaining("不能为 null");
        }

        @Test
        @DisplayName("should throw when productPaths is null")
        void validate_withNullProductPaths_shouldThrow() {
            properties.setProductPaths(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("productPaths");
        }

        @Test
        @DisplayName("should throw when staticPaths is null")
        void validate_withNullStaticPaths_shouldThrow() {
            properties.setStaticPaths(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("staticPaths");
        }

        @Test
        @DisplayName("should throw when allowedOrigins is null")
        void validate_withNullAllowedOrigins_shouldThrow() {
            properties.setAllowedOrigins(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("allowedOrigins");
        }

        @Test
        @DisplayName("should throw when password encoder strength is below 4")
        void validate_withLowPasswordStrength_shouldThrow() {
            properties.setPasswordEncoderStrength(3);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("密码加密强度必须在 4-31 之间");
        }

        @Test
        @DisplayName("should throw when password encoder strength is above 31")
        void validate_withHighPasswordStrength_shouldThrow() {
            properties.setPasswordEncoderStrength(32);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("密码加密强度必须在 4-31 之间");
        }

        @Test
        @DisplayName("should pass with valid configuration")
        void validate_withValidConfig_shouldPass() {
            properties.setIgnorePaths(List.of("/api/public/**"));
            properties.setProductPaths(List.of("/api/products/**"));
            properties.setStaticPaths(List.of("/static/**"));
            properties.setAllowedOrigins(List.of("https://example.com"));
            properties.setPasswordEncoderStrength(12);

            // Should not throw
            properties.validate();
        }

        @Test
        @DisplayName("should warn but not throw for CORS wildcard")
        void validate_withAllowedOriginsWildcard_shouldNotThrow() {
            properties.setAllowedOrigins(List.of("*"));

            // Should not throw, only logs warning
            properties.validate();
        }

        @Test
        @DisplayName("should warn but not throw for low password strength")
        void validate_withLowStrengthWarning_shouldNotThrow() {
            properties.setPasswordEncoderStrength(8);

            // Should not throw (only logs warning)
            properties.validate();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("getIgnorePaths should return unmodifiable list")
        void getIgnorePaths_shouldBeUnmodifiable() {
            properties.setIgnorePaths(List.of("/api/public"));

            List<String> paths = properties.getIgnorePaths();

            assertThatThrownBy(() -> paths.add("/api/other"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getProductPaths should return unmodifiable list")
        void getProductPaths_shouldBeUnmodifiable() {
            properties.setProductPaths(List.of("/api/products/**"));

            List<String> paths = properties.getProductPaths();

            assertThatThrownBy(() -> paths.add("/api/other"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getStaticPaths should return unmodifiable list")
        void getStaticPaths_shouldBeUnmodifiable() {
            properties.setStaticPaths(List.of("/static/**"));

            List<String> paths = properties.getStaticPaths();

            assertThatThrownBy(() -> paths.add("/api/other"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getAllowedOrigins should return unmodifiable list")
        void getAllowedOrigins_shouldBeUnmodifiable() {
            properties.setAllowedOrigins(List.of("https://example.com"));

            List<String> origins = properties.getAllowedOrigins();

            assertThatThrownBy(() -> origins.add("https://other.com"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
