package com.gobro_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for generating, parsing and validating JWT
 * access and refresh tokens used to authenticate REST API calls.
 *
 * A valid access token carries the username (subject), the user's
 * roles/authorities and standard iat/exp claims. A refresh token
 * carries only the subject and a longer expiration, and is used
 * solely to obtain a new access token via /api/v1/auth/refresh.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration-ms:900000}") // 15 minutes
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TOKEN_TYPE = "type";

    public String generateAccessToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_TYPE, "access");
        return buildToken(claims, userDetails.getUsername(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, "refresh");
        return buildToken(claims, userDetails.getUsername(), refreshTokenExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) extractAllClaims(token).get(CLAIM_ROLES);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object id = claims.get(CLAIM_USER_ID);
            if (id == null) return null;
            if (id instanceof Number) return ((Number) id).longValue();
            try {
                return Long.parseLong(id.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        });
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class)));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}