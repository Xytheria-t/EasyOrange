package com.cartethyia.easyorange.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.testsupport.PropertyBindings;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtProperties Tests")
class JwtPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<ConstraintViolation<JwtProperties>> violations(JwtProperties properties) {
        return validator.validate(properties);
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        /** 空属性源绑定结果 —— 默认值来自 @DefaultValue，测试不重复写默认值。 */
        private final JwtProperties properties = PropertyBindings.bind(JwtProperties.class);

        @Test
        @DisplayName("should have default private key location as empty string")
        void privateKeyLocation_default_shouldBeEmpty() {
            assertThat(properties.privateKeyLocation()).isEmpty();
        }

        @Test
        @DisplayName("should have default public key location as empty string")
        void publicKeyLocation_default_shouldBeEmpty() {
            assertThat(properties.publicKeyLocation()).isEmpty();
        }

        @Test
        @DisplayName("should have default access token expiration of 30 minutes")
        void accessTokenExpiration_default_shouldBe30() {
            assertThat(properties.accessTokenExpiration()).isEqualTo(30L);
        }

        @Test
        @DisplayName("should have default refresh token expiration of 7 days")
        void refreshTokenExpiration_default_shouldBe7() {
            assertThat(properties.refreshTokenExpiration()).isEqualTo(7L);
        }

        @Test
        @DisplayName("should have default issuer 'easyorange'")
        void issuer_default_shouldBeEasyorange() {
            assertThat(properties.issuer()).isEqualTo("easyorange");
        }
    }

    @Nested
    @DisplayName("validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject null issuer")
        void nullIssuer_shouldHaveViolation() {
            // 属性源表达不了 null（null 值等于缺省，会被 @DefaultValue 兜底），只能直接构造出 null 组件
            var properties = new JwtProperties("", "", 30L, 7L, null, "eo_refresh_token", "/api/auth", true, "Lax");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("发行者"));
        }

        @Test
        @DisplayName("should reject blank issuer")
        void blankIssuer_shouldHaveViolation() {
            var properties = PropertyBindings.bind(JwtProperties.class, "issuer", "   ");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("发行者"));
        }

        @Test
        @DisplayName("should reject zero access token expiration")
        void zeroAccessTokenExpiration_shouldHaveViolation() {
            var properties = PropertyBindings.bind(JwtProperties.class, "access-token-expiration", "0");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("Access Token"));
        }

        @Test
        @DisplayName("should reject negative access token expiration")
        void negativeAccessTokenExpiration_shouldHaveViolation() {
            var properties = PropertyBindings.bind(JwtProperties.class, "access-token-expiration", "-1");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("Access Token"));
        }

        @Test
        @DisplayName("should reject zero refresh token expiration")
        void zeroRefreshTokenExpiration_shouldHaveViolation() {
            var properties = PropertyBindings.bind(JwtProperties.class, "refresh-token-expiration", "0");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("Refresh Token"));
        }

        @Test
        @DisplayName("should reject negative refresh token expiration")
        void negativeRefreshTokenExpiration_shouldHaveViolation() {
            var properties = PropertyBindings.bind(JwtProperties.class, "refresh-token-expiration", "-1");

            assertThat(violations(properties)).anyMatch(v -> v.getMessage().contains("Refresh Token"));
        }

        @Test
        @DisplayName("should pass with valid defaults")
        void validDefaults_shouldHaveNoViolations() {
            assertThat(violations(PropertyBindings.bind(JwtProperties.class))).isEmpty();
        }

        @Test
        @DisplayName("should pass with empty key locations")
        void emptyKeyLocations_shouldHaveNoViolations() {
            var properties =
                    PropertyBindings.bind(JwtProperties.class, "private-key-location", "", "public-key-location", "");

            assertThat(violations(properties)).isEmpty();
        }

        @Test
        @DisplayName("should pass with configured key locations")
        void configuredKeyLocations_shouldHaveNoViolations() {
            var properties = PropertyBindings.bind(
                    JwtProperties.class,
                    "private-key-location",
                    "classpath:keys/private.pem",
                    "public-key-location",
                    "classpath:keys/public.pem");

            assertThat(violations(properties)).isEmpty();
        }

        @Test
        @DisplayName("should pass with explicit valid values")
        void explicitValidValues_shouldHaveNoViolations() {
            var properties = PropertyBindings.bind(
                    JwtProperties.class,
                    "issuer",
                    "easyorange",
                    "access-token-expiration",
                    "30",
                    "refresh-token-expiration",
                    "7");

            assertThat(violations(properties)).isEmpty();
        }
    }
}
