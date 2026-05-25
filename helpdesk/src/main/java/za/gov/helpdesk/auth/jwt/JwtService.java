package za.gov.helpdesk.auth.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.security.CustomUserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    public String generateAccessToken(User  user) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("type", "access");

        return buildToken(claims, user.getEmail(), accessTokenExpiryMs);
    }

    public String generateRefreshToken(User  user) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(claims,user.getEmail(), refreshTokenExpiryMs);
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

    public boolean isTokenValid(String token, UserDetails user) {
        final String email = extractEmail(token);

        return email.equals(user.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

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
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
