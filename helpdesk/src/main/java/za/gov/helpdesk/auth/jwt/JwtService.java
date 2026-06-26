package za.gov.helpdesk.auth.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import za.gov.helpdesk.exception.InvalidTokenException;
import za.gov.helpdesk.users.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Service component responsible for JSON Web Token (JWT) management operations. Handles
 * cryptographic signing key initialization, token generation for access and refresh contexts,
 * signature parsing, expiration checking, and subject extraction.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    /**
     * Generates a short-lived cryptographically signed JWT access token for a user. Attaches
     * authorized role permissions and identifies the token purpose as "access".
     *
     * @param user the domain {@link User} details context to pack into the claims set
     * @return a signed compact JWT serialization string
     */
    public String generateAccessToken(final User user) {

        final Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("type", "access");

        return buildToken(claims, user.getEmail(), accessTokenExpiryMs);
    }

    /**
     * Generates a long-lived signed JWT refresh token for session tracking boundaries. Minimizes
     * structural payload overhead and sets the token purpose marker as "refresh".
     *
     * @param user the domain {@link User} details context to pack into the claims set
     * @return a signed compact JWT serialization string
     */
    public String generateRefreshToken(final User user) {

        final Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(claims, user.getEmail(), refreshTokenExpiryMs);
    }

    /**
     * Compiles, signs, and flattens a set of custom claims, a subject identification string, and
     * expiration intervals into a secure JWT string wrapper.
     *
     * @param claims custom fields to attach inside the JSON payload space
     * @param subject the username identity handle (email address) representing the principal
     * @param expiryMs total duration interval in milliseconds before token expiration
     * @return a compressed, signed JWT string
     */
    private String buildToken(
            final Map<String, Object> claims, final String subject, final Long expiryMs) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Asserts if a token is cryptographically sound and matches the username identity criteria.
     * Checks token signature accuracy, claims integrity, and validity duration boundaries.
     *
     * @param token the raw JWT token string to evaluate
     * @param user the {@link UserDetails} core authentication abstraction mapping the user
     *     properties
     * @return true if the token subject matches the principal identity and is unexpired, false
     *     otherwise
     */
    public boolean isTokenValid(final String token, final UserDetails user) {
        final String email = extractEmail(token);

        return email.equals(user.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Validates whether the given token contains a dedicated "refresh" token type classification.
     *
     * @param token the raw JWT token string to evaluate
     * @return true if the internal metadata type claim evaluates exactly to "refresh", false
     *     otherwise
     */
    public boolean isRefreshToken(final String token) {
        return "refresh".equals(extractTokenType(token));
    }

    /**
     * Determines whether the current machine time has surpassed the token's expiration date
     * metadata.
     *
     * @param token the raw JWT token string to evaluate
     * @return true if the token window expiration has lapsed, false otherwise
     */
    public boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the subject identity parameter (email handle) encapsulated within the token payload.
     *
     * @param token the raw JWT token string to open
     * @return the string email handle packed into the token subject claim field
     */
    public String extractEmail(final String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the dedicated system operational type category ("access" or "refresh") from the
     * token payload.
     *
     * @param token the raw JWT token string to open
     * @return the type string identifier property matching the token configuration purpose
     */
    public String extractTokenType(final String token) {
        return extractClaims(token).get("type").toString();
    }

    /**
     * Extracts the absolute date snapshot indicating exactly when the token parameters become
     * obsolete.
     *
     * @param token the raw JWT token string to open
     * @return the absolute {@link Date} boundary tracking token expiration
     */
    public Date extractExpiration(final String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Parses and decodes a signed JWT string token, validating its cryptographic signature
     * integrity. Extracts the core claims set payload from the decrypted token payload data frame.
     *
     * @param token the signed compact JWT string target to decrypt and parse
     * @return the underlying verified {@link Claims} instance data set map
     * @throws InvalidTokenException if the cryptographic signature is broken, altered, or structure
     *     is malformed
     */
    public Claims extractClaims(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (final JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or malformed refresh token structure", e);
        }
    }

    /**
     * Decodes the Base64 encoded raw application secret property configuration key. Generates an
     * HMAC-SHA standard compatible cryptographic signing key instance.
     *
     * @return the secure {@link SecretKey} engine used for signing and verifying tokens
     */
    private SecretKey getSigningKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
