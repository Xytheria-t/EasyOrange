package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    private static final String SECRET_KEY;

    static {
        String base64 = Base64.getEncoder().encodeToString(new byte[32]);
        SECRET_KEY = base64 + base64; // 64 chars >= 32
    }

    private static final String ISSUER = "test-issuer";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(SECRET_KEY);
        jwtProperties.setIssuer(ISSUER);
        jwtProperties.setAccessTokenExpiration(60L);
        jwtProperties.setRefreshTokenExpiration(7L);
        jwtProperties.setAutoRenewThresholdMinutes(5L);
        jwtProperties.setTokenPrefix("Bearer ");

        jwtUtil = new JwtUtil(jwtProperties);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("should generate a valid JWT token")
        void generateToken_withSubjectAndClaims_shouldReturnToken() {
            String token = jwtUtil.generateToken("user123", Map.of("role", "admin"));

            assertThat(token).isNotBlank();
            assertThat(token).doesNotContain("Bearer ");
        }

        @Test
        @DisplayName("should generate token with custom expiration")
        void generateToken_withCustomExpiration_shouldReturnToken() {
            String token = jwtUtil.generateToken("user123", Map.of(), 30L);

            assertThat(token).isNotBlank();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("should generate a refresh token with type claim")
        void generateRefreshToken_shouldContainTypeClaim() {
            String token = jwtUtil.generateRefreshToken("user123", Map.of("role", "admin"));

            assertThat(token).isNotBlank();
            Optional<Claims> parsed = jwtUtil.parseToken(token);
            assertThat(parsed).isPresent();
            assertThat(parsed.get().get("type", String.class)).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("parseToken")
    class ParseTokenTests {

        @Test
        @DisplayName("should parse a valid token")
        void parseToken_withValidToken_shouldReturnClaims() {
            String token = jwtUtil.generateToken("user123", Map.of("role", "admin"));

            Optional<Claims> result = jwtUtil.parseToken(token);

            assertThat(result).isPresent();
            assertThat(result.get().getSubject()).isEqualTo("user123");
            assertThat(result.get().get("role", String.class)).isEqualTo("admin");
            assertThat(result.get().getIssuer()).isEqualTo(ISSUER);
        }

        @Test
        @DisplayName("should parse token with Bearer prefix")
        void parseToken_withBearerPrefix_shouldStripAndParse() {
            String rawToken = jwtUtil.generateToken("user123", Map.of());
            String bearerToken = "Bearer " + rawToken;

            Optional<Claims> result = jwtUtil.parseToken(bearerToken);

            assertThat(result).isPresent();
            assertThat(result.get().getSubject()).isEqualTo("user123");
        }

        @Test
        @DisplayName("should return empty for null token")
        void parseToken_withNullToken_shouldReturnEmpty() {
            Optional<Claims> result = jwtUtil.parseToken(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for blank token")
        void parseToken_withBlankToken_shouldReturnEmpty() {
            Optional<Claims> result = jwtUtil.parseToken("   ");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for invalid token")
        void parseToken_withInvalidToken_shouldReturnEmpty() {
            Optional<Claims> result = jwtUtil.parseToken("invalid.jwt.token");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for expired token")
        void parseToken_withExpiredToken_shouldReturnEmpty() throws Exception {
            jwtProperties.setAccessTokenExpiration(0L);
            jwtUtil = new JwtUtil(jwtProperties);
            String token = jwtUtil.generateToken("user123", Map.of());
            Thread.sleep(10);

            Optional<Claims> result = jwtUtil.parseToken(token);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseTokenIgnoreExpiration")
    class ParseTokenIgnoreExpirationTests {

        @Test
        @DisplayName("should parse valid token")
        void parseTokenIgnoreExpiration_withValidToken_shouldReturnClaims() {
            String token = jwtUtil.generateToken("user123", Map.of());

            Optional<Claims> result = jwtUtil.parseTokenIgnoreExpiration(token);

            assertThat(result).isPresent();
            assertThat(result.get().getSubject()).isEqualTo("user123");
        }

        @Test
        @DisplayName("should return claims for expired token")
        void parseTokenIgnoreExpiration_withExpiredToken_shouldReturnClaims() throws Exception {
            jwtProperties.setAccessTokenExpiration(0L);
            jwtUtil = new JwtUtil(jwtProperties);
            String token = jwtUtil.generateToken("user123", Map.of());
            Thread.sleep(10);

            Optional<Claims> result = jwtUtil.parseTokenIgnoreExpiration(token);

            assertThat(result).isPresent();
            assertThat(result.get().getSubject()).isEqualTo("user123");
        }

        @Test
        @DisplayName("should return empty for invalid signature")
        void parseTokenIgnoreExpiration_withInvalidSignature_shouldReturnEmpty() {
            Optional<Claims> result = jwtUtil.parseTokenIgnoreExpiration("invalid.jwt.token");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null token")
        void parseTokenIgnoreExpiration_withNullToken_shouldReturnEmpty() {
            Optional<Claims> result = jwtUtil.parseTokenIgnoreExpiration(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return true for valid token")
        void validateToken_withValidToken_shouldReturnTrue() {
            String token = jwtUtil.generateToken("user123", Map.of());

            boolean result = jwtUtil.validateToken(token);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for null token")
        void validateToken_withNullToken_shouldReturnFalse() {
            boolean result = jwtUtil.validateToken(null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for blank token")
        void validateToken_withBlankToken_shouldReturnFalse() {
            boolean result = jwtUtil.validateToken("   ");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for invalid token")
        void validateToken_withInvalidToken_shouldReturnFalse() {
            boolean result = jwtUtil.validateToken("invalid.jwt.token");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for expired token")
        void validateToken_withExpiredToken_shouldReturnFalse() throws Exception {
            jwtProperties.setAccessTokenExpiration(0L);
            jwtUtil = new JwtUtil(jwtProperties);
            String token = jwtUtil.generateToken("user123", Map.of());
            Thread.sleep(10);

            boolean result = jwtUtil.validateToken(token);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getSubject")
    class GetSubjectTests {

        @Test
        @DisplayName("should return subject from valid token")
        void getSubject_withValidToken_shouldReturnSubject() {
            String token = jwtUtil.generateToken("user123", Map.of());

            Optional<String> subject = jwtUtil.getSubject(token);

            assertThat(subject).contains("user123");
        }

        @Test
        @DisplayName("should return empty for invalid token")
        void getSubject_withInvalidToken_shouldReturnEmpty() {
            Optional<String> subject = jwtUtil.getSubject("invalid");

            assertThat(subject).isEmpty();
        }
    }

    @Nested
    @DisplayName("getClaim")
    class GetClaimTests {

        @Test
        @DisplayName("should return custom claim from valid token")
        void getClaim_withValidToken_shouldReturnClaim() {
            String token = jwtUtil.generateToken("user123", Map.of("role", "admin"));

            Optional<String> role = jwtUtil.getClaim(token, "role", String.class);

            assertThat(role).contains("admin");
        }

        @Test
        @DisplayName("should return empty for non-existent claim")
        void getClaim_withMissingClaim_shouldReturnEmpty() {
            String token = jwtUtil.generateToken("user123", Map.of());

            Optional<String> result = jwtUtil.getClaim(token, "nonexistent", String.class);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("renewTokenIfNeeded")
    class RenewTokenIfNeededTests {

        @Test
        @DisplayName("should not renew a non-expiring token")
        void renewTokenIfNeeded_withFreshToken_shouldReturnEmpty() {
            String token = jwtUtil.generateToken("user123", Map.of("role", "admin"), 60L);

            Optional<String> renewed = jwtUtil.renewTokenIfNeeded(token);

            assertThat(renewed).isEmpty();
        }

        @Test
        @DisplayName("should not renew a refresh token")
        void renewTokenIfNeeded_withRefreshToken_shouldReturnEmpty() {
            String token = jwtUtil.generateRefreshToken("user123", Map.of());

            Optional<String> renewed = jwtUtil.renewTokenIfNeeded(token);

            assertThat(renewed).isEmpty();
        }

        @Test
        @DisplayName("should return empty for invalid token")
        void renewTokenIfNeeded_withInvalidToken_shouldReturnEmpty() {
            Optional<String> renewed = jwtUtil.renewTokenIfNeeded("invalid");

            assertThat(renewed).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeBearerPrefix")
    class RemoveBearerPrefixTests {

        @Test
        @DisplayName("should strip Bearer prefix from token")
        void removeBearerPrefix_withBearerPrefix_shouldStrip() {
            String token = jwtUtil.generateToken("user123", Map.of());
            String bearerToken = "Bearer " + token;

            Optional<Claims> result = jwtUtil.parseToken(bearerToken);

            assertThat(result).isPresent();
            assertThat(result.get().getSubject()).isEqualTo("user123");
        }

        @Test
        @DisplayName("should handle token without prefix")
        void removeBearerPrefix_withoutPrefix_shouldReturnSame() {
            String token = jwtUtil.generateToken("user123", Map.of());

            Optional<Claims> result = jwtUtil.parseToken(token);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("InvalidSignature")
    class InvalidSignatureTests {

        @Test
        @DisplayName("should reject token signed with different key")
        void parseToken_withWrongKey_shouldReturnEmpty() {
            SecretKey otherKey = Keys.hmacShaKeyFor(
                    "different-secret-key-that-is-at-least-32-characters-long!!".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("user123")
                    .issuer(ISSUER)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(otherKey)
                    .compact();

            Optional<Claims> result = jwtUtil.parseToken(token);

            assertThat(result).isEmpty();
        }
    }
}
