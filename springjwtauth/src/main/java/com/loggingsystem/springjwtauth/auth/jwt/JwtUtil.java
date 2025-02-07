package com.loggingsystem.springjwtauth.auth.jwt;

import com.loggingsystem.springjwtauth.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * A utility class for creating and validating JSON Web Tokens (JWTs).
 * This class handles JWT generation, validation, and claims extraction using a secret key.
 */
@Component
@Slf4j
public class JwtUtil {
    private final JwtProperties jwtProperties;

    /**
     * Constructor for injecting JWT properties.
     *
     * @param jwtProperties the JWT configuration properties.
     */
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generates a JWT for the given user.
     *
     * @param user the user for whom the token is generated.
     * @return the generated JWT as a string.
     */
    public String generateToken(User user) {
        return Jwts
                .builder()
                .subject(user.getUsername())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getValidity())) //one day
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts claims from the provided JWT.
     *
     * @param token the JWT from which claims are extracted.
     * @return the claims extracted from the token.
     */
    public Claims getClaims(String token) {
        log.info("in claims");

        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    /**
     * Validates the provided JWT by checking if it is expired.
     *
     * @param token the JWT to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token) {
        return !isExpired(token);
    }

    /**
     * Checks if the provided JWT is expired.
     *
     * @param token the JWT to check.
     * @return true if the token is expired, false otherwise.
     */
    private boolean isExpired(String token) {
        log.info("isExpired");

        return  getClaims(token)
                .getExpiration()
                .before(new Date());

    }

    /**
     * Decodes the secret key from Base64 and generates a HMAC SHA key.
     *
     * @return the generated secret key.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        Claims claims = getClaims(token);

        return claims.getSubject();
    }
}
