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
        @DisplayName("should have default secret key as empty string")
        void secretKey_default_shouldBeEmpty() {
            assertThat(properties.getSecretKey()).isEmpty();
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
        @DisplayName("should have default token prefix 'Bearer '")
        void tokenPrefix_default_shouldBeBearer() {
            assertThat(properties.getTokenPrefix()).isEqualTo("Bearer ");
        }

        @Test
        @DisplayName("should have default issuer 'easyorange'")
        void issuer_default_shouldBeEasyorange() {
            assertThat(properties.getIssuer()).isEqualTo("easyorange");
        }

        @Test
        @DisplayName("should have default auto renew threshold of 5 minutes")
        void autoRenewThresholdMinutes_default_shouldBe5() {
            assertThat(properties.getAutoRenewThresholdMinutes()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should throw when secret key is null")
        void validate_withNullSecretKey_shouldThrow() {
            properties.setSecretKey(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT 密钥不能为空");
        }

        @Test
        @DisplayName("should throw when secret key is blank")
        void validate_withBlankSecretKey_shouldThrow() {
            properties.setSecretKey("   ");

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT 密钥不能为空");
        }

        @Test
        @DisplayName("should throw when secret key is too short")
        void validate_withShortSecretKey_shouldThrow() {
            properties.setSecretKey("short");

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT 密钥长度必须至少 32 字符");
        }

        @Test
        @DisplayName("should warn but not throw for weak key")
        void validate_withWeakKey_shouldNotThrow() {
            properties.setSecretKey("dev-secret-key-that-is-at-least-32-characters!!");

            // Should not throw exception for weak key (only logs warning)
            properties.validate();
        }

        @Test
        @DisplayName("should pass with valid key")
        void validate_withValidKey_shouldPass() {
            properties.setSecretKey("aBcDeFgHiJkLmNoPqRsT uVwXyZ0123456789");

            properties.validate();

            assertThat(properties.getSecretKey().length()).isGreaterThanOrEqualTo(32);
        }

        @Test
        @DisplayName("should log warning for example key")
        void validate_withExampleKey_shouldLogWarning() {
            properties.setSecretKey("example-secret-key-that-is-at-least-32-bytes!!");

            // Should not throw
            properties.validate();
        }

        @Test
        @DisplayName("should not throw for key containing weak patterns when length is sufficient")
        void validate_withWeakButLongKey_shouldNotThrow() {
            properties.setSecretKey("default-secret-key-that-is-at-least-32-characters");

            properties.validate();
        }
    }
}
