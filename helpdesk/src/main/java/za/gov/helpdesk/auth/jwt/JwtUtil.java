package za.gov.helpdesk.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.UnknownNullability;
import org.springframework.security.core.userdetails.UserDetails;
import za.gov.helpdesk.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.employee.model.Employees;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for creating and validating JSON Web Tokens (JWTs).
 * This class handles JWT generation, validation, and claims extraction using a secret key.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
    private final JwtProperties jwtProperties;

    // ── Token Generation ──────────────────────────────────────
    public String generateAccessToken(Employees user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("type", "access");

        return buildToken(claims, user.getUsername(), jwtProperties.getValidity());
    }

    public String generateRefreshToken(@UnknownNullability Employees user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(claims,user.getUsername(), jwtProperties.getRefreshValidity());
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expiryMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token Validation ──────────────────────────────────────
    public boolean isTokenValid(String token, UserDetails user) {
        final String email = extractEmail(token);

        return email.equals(user.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Token Validation ──────────────────────────────────────
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type").toString();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }
    public Claims extractClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
