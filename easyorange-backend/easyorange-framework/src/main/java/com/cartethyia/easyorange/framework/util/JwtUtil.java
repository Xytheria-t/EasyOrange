package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
enum ReservedClaim {
    ISS("iss"), SUB("sub"), AUD("aud"), EXP("exp"), NBF("nbf"), IAT("iat"), JTI("jti");

    private final String value;

    ReservedClaim(String value) {
        this.value = value;
    }

    public static boolean contains(String claim) {
        return CLAIM_SET.contains(claim);
    }

    private static final Set<String> CLAIM_SET = EnumSet.allOf(ReservedClaim.class)
            .stream().map(ReservedClaim::getValue).collect(Collectors.toSet());
}

@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build();
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, jwtProperties.getAccessTokenExpiration());
    }

    public String generateToken(String subject, Map<String, Object> claims, long expirationMinutes) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofMinutes(expirationMinutes));

        JwtBuilder builder = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey);

        builder.claims(claims);

        return builder.compact();
    }

    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        long expirationDays = jwtProperties.getRefreshTokenExpiration();
        Map<String, Object> refreshClaims = new java.util.HashMap<>(claims);
        refreshClaims.put(CLAIM_TYPE, REFRESH_TOKEN_TYPE);

        return generateToken(subject, refreshClaims, expirationDays * 24 * 60);
    }

    public Optional<Claims> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String cleanToken = removeBearerPrefix(token);

        try {
            return Optional.of(jwtParser.parseSignedClaims(cleanToken).getPayload());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<Claims> parseTokenIgnoreExpiration(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String cleanToken = removeBearerPrefix(token);

        try {
            return Optional.of(jwtParser.parseSignedClaims(cleanToken).getPayload());
        } catch (ExpiredJwtException e) {
            return Optional.of(e.getClaims());
        } catch (SignatureException e) {
            log.error("JWT parseIgnoreExp - signature verification failed: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT parseIgnoreExp - token parsing failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String cleanToken = removeBearerPrefix(token);
        try {
            jwtParser.parseSignedClaims(cleanToken);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Optional<String> getSubject(String token) {
        return parseToken(token).map(Claims::getSubject);
    }

    public <T> Optional<T> getClaim(String token, String claimName, Class<T> claimType) {
        return parseToken(token).map(claims -> claims.get(claimName, claimType));
    }

    public Optional<String> renewTokenIfNeeded(String token) {
        return parseTokenIgnoreExpiration(token)
                .filter(claims -> !REFRESH_TOKEN_TYPE.equals(claims.get(CLAIM_TYPE, String.class)))
                .filter(this::isNearExpiration)
                .map(claims -> generateToken(claims.getSubject(), extractCustomClaims(claims)));
    }

    private boolean isNearExpiration(Claims claims) {
        long remainingMinutes = Duration.between(Instant.now(), claims.getExpiration().toInstant()).toMinutes();
        return remainingMinutes <= jwtProperties.getAutoRenewThresholdMinutes();
    }

    private Map<String, Object> extractCustomClaims(Claims claims) {
        return claims.entrySet().stream()
                .filter(e -> !ReservedClaim.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String removeBearerPrefix(String token) {
        String prefix = jwtProperties.getTokenPrefix();
        if (token.startsWith(prefix)) {
            return token.substring(prefix.length());
        }
        return token;
    }
}
