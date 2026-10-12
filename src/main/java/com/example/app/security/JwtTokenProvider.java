package com.example.app.security;

import com.example.app.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final long resetTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs,
            @Value("${app.jwt.reset-token-expiration-ms:600000}") long resetTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
        this.resetTokenExpirationMs = resetTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String role = user.getRole() != null ? user.getRole().name() : "";
        String permissions = user.getPermissions() != null
                ? user.getPermissions().stream().map(Enum::name).collect(Collectors.joining(","))
                : "";

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("permissions", permissions)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generatePasswordResetToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + resetTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("type", "PASSWORD_RESET")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("type", String.class) : null;
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims != null && "ACCESS".equals(claims.get("type", String.class));
        } catch (Exception e) {
            log.error("Invalid JWT access token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validatePasswordResetToken(String token, String expectedEmail) {
        try {
            Claims claims = getClaims(token);
            if (claims == null) return false;
            boolean isReset = "PASSWORD_RESET".equals(claims.get("type", String.class));
            boolean emailMatches = expectedEmail != null && expectedEmail.equalsIgnoreCase(claims.getSubject());
            return isReset && emailMatches;
        } catch (Exception e) {
            log.error("Invalid password reset token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT parsing error: {}", e.getMessage());
            return null;
        }
    }
}
