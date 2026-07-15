package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties Tests")
class JwtPropertiesTest {

    private static Validator validator;

    private JwtProperties properties;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
    }

    private Set<ConstraintViolation<JwtProperties>> violations() {
        return validator.validate(properties);
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
    @DisplayName("validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject null issuer")
        void nullIssuer_shouldHaveViolation() {
            properties.setIssuer(null);
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("发行者"));
        }

        @Test
        @DisplayName("should reject blank issuer")
        void blankIssuer_shouldHaveViolation() {
            properties.setIssuer("   ");
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("发行者"));
        }

        @Test
        @DisplayName("should reject zero access token expiration")
        void zeroAccessTokenExpiration_shouldHaveViolation() {
            properties.setAccessTokenExpiration(0);
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("Access Token"));
        }

        @Test
        @DisplayName("should reject negative access token expiration")
        void negativeAccessTokenExpiration_shouldHaveViolation() {
            properties.setAccessTokenExpiration(-1);
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("Access Token"));
        }

        @Test
        @DisplayName("should reject zero refresh token expiration")
        void zeroRefreshTokenExpiration_shouldHaveViolation() {
            properties.setRefreshTokenExpiration(0);
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("Refresh Token"));
        }

        @Test
        @DisplayName("should reject negative refresh token expiration")
        void negativeRefreshTokenExpiration_shouldHaveViolation() {
            properties.setRefreshTokenExpiration(-1);
            assertThat(violations())
                    .anyMatch(v -> v.getMessage().contains("Refresh Token"));
        }

        @Test
        @DisplayName("should pass with valid defaults")
        void validDefaults_shouldHaveNoViolations() {
            assertThat(violations()).isEmpty();
        }

        @Test
        @DisplayName("should pass with empty key locations")
        void emptyKeyLocations_shouldHaveNoViolations() {
            properties.setPrivateKeyLocation("");
            properties.setPublicKeyLocation("");
            assertThat(violations()).isEmpty();
        }

        @Test
        @DisplayName("should pass with configured key locations")
        void configuredKeyLocations_shouldHaveNoViolations() {
            properties.setPrivateKeyLocation("classpath:keys/private.pem");
            properties.setPublicKeyLocation("classpath:keys/public.pem");
            assertThat(violations()).isEmpty();
        }

        @Test
        @DisplayName("should pass with explicit valid values")
        void explicitValidValues_shouldHaveNoViolations() {
            properties.setIssuer("easyorange");
            properties.setAccessTokenExpiration(30);
            properties.setRefreshTokenExpiration(7);
            assertThat(violations()).isEmpty();
        }
    }
}
