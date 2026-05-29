package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, jwtProperties.getAccessTokenExpiration());
    }

    public String generateToken(String subject, Map<String, Object> claims, long expirationMinutes) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofMinutes(expirationMinutes));

        var builder = Jwts.builder()
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
            var parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build();
            return Optional.of(parser.parseSignedClaims(cleanToken).getPayload());
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
            var parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build();
            return Optional.of(parser.parseSignedClaims(cleanToken).getPayload());
        } catch (ExpiredJwtException e) {
            return Optional.of(e.getClaims());
        } catch (JwtException e) {
            log.error("JWT parseIgnoreExp - 解析失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String cleanToken = removeBearerPrefix(token);
        try {
            var parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build();
            parser.parseSignedClaims(cleanToken);
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

    /**
     * 从 Authorization header 中提取 Bearer token。
     * 如果 header 为空或不含 Bearer 前缀，返回 null。
     */
    public String extractToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (!authHeader.startsWith(prefix)) {
            return null;
        }
        return authHeader.substring(prefix.length());
    }

    private String removeBearerPrefix(String token) {
        String prefix = jwtProperties.getTokenPrefix();
        if (token.startsWith(prefix)) {
            return token.substring(prefix.length());
        }
        return token;
    }
}
