package com.cartethyia.easyorange.framework.config.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtProperties Tests")
class JwtPropertiesTest {

    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have default private key location as empty string")
        void privateKeyLocation_default_shouldBeEmpty() {
            assertThat(properties.getPrivateKeyLocation()).isEmpty();
        }

        @Test
        @DisplayName("should have default public key location as empty string")
        void publicKeyLocation_default_shouldBeEmpty() {
            assertThat(properties.getPublicKeyLocation()).isEmpty();
        }

        @Test
        @DisplayName("should have default access token expiration of 30 minutes")
        void accessTokenExpiration_default_shouldBe30() {
            assertThat(properties.getAccessTokenExpiration()).isEqualTo(30L);
        }

        @Test
        @DisplayName("should have default refresh token expiration of 7 days")
        void refreshTokenExpiration_default_shouldBe7() {
            assertThat(properties.getRefreshTokenExpiration()).isEqualTo(7L);
        }

        @Test
        @DisplayName("should have default issuer 'easyorange'")
        void issuer_default_shouldBeEasyorange() {
            assertThat(properties.getIssuer()).isEqualTo("easyorange");
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should throw when issuer is null")
        void validate_withNullIssuer_shouldThrow() {
            properties.setIssuer(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("发行者");
        }

        @Test
        @DisplayName("should throw when issuer is blank")
        void validate_withBlankIssuer_shouldThrow() {
            properties.setIssuer("   ");

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("发行者");
        }

        @Test
        @DisplayName("should throw when access token expiration is zero")
        void validate_withZeroAccessTokenExpiration_shouldThrow() {
            properties.setAccessTokenExpiration(0);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("accessTokenExpiration");
        }

        @Test
        @DisplayName("should throw when access token expiration is negative")
        void validate_withNegativeAccessTokenExpiration_shouldThrow() {
            properties.setAccessTokenExpiration(-1);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("accessTokenExpiration");
        }

        @Test
        @DisplayName("should throw when refresh token expiration is zero")
        void validate_withZeroRefreshTokenExpiration_shouldThrow() {
            properties.setRefreshTokenExpiration(0);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("refreshTokenExpiration");
        }

        @Test
        @DisplayName("should throw when refresh token expiration is negative")
        void validate_withNegativeRefreshTokenExpiration_shouldThrow() {
            properties.setRefreshTokenExpiration(-1);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("refreshTokenExpiration");
        }

        @Test
        @DisplayName("should pass with valid configuration")
        void validate_withValidConfig_shouldPass() {
            properties.setIssuer("easyorange");
            properties.setAccessTokenExpiration(30);
            properties.setRefreshTokenExpiration(7);

            properties.validate();
        }

        @Test
        @DisplayName("should pass with empty key locations (dev auto-generate)")
        void validate_withEmptyKeyLocations_shouldNotThrow() {
            properties.setPrivateKeyLocation("");
            properties.setPublicKeyLocation("");

            properties.validate();
        }

        @Test
        @DisplayName("should pass with configured key locations")
        void validate_withConfiguredKeyLocations_shouldNotThrow() {
            properties.setPrivateKeyLocation("classpath:keys/private.pem");
            properties.setPublicKeyLocation("classpath:keys/public.pem");

            properties.validate();
        }
    }
}
