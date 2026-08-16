package com.example.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JWTTokenProvider {

    @Value("${question.expires.in}")
    private long EXPIRES_IN;

    private final SecretKey key;

    /**
     * The signing key comes from configuration rather than being generated per boot.
     *
     * Generating it (Keys.secretKeyFor, as this did) meant every restart silently invalidated every
     * token in the wild, and made running a second instance impossible -- whichever one received the
     * request would reject a token the other had signed. Neither shows up locally, where there is
     * one process and a restart just means logging in again.
     *
     * @throws IllegalStateException at startup if the key is missing or too short. Failing here is
     *                               deliberate: the alternative is booting with an absent or weak
     *                               key and finding out when someone forges a token.
     */
    public JWTTokenProvider(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is not set. Generate one with `openssl rand -base64 32` and "
                            + "pass it as the JWT_SECRET environment variable.");
        }

        byte[] material = Base64.getDecoder().decode(secret.trim());
        // HS256 needs 256 bits. jjwt would reject a shorter key itself, but with a stack trace that
        // does not say what to do about it.
        if (material.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret decodes to " + material.length * 8 + " bits; HS256 needs at "
                            + "least 256. Generate one with `openssl rand -base64 32`.");
        }
        this.key = Keys.hmacShaKeyFor(material);
    }

    public String generateJWTToken(Authentication auth) {
        JWTUserDetails userDetails = (JWTUserDetails) auth.getPrincipal();
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRES_IN);

        return Jwts.builder()
                .setSubject(Long.toString(userDetails.getId()))
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateJwtTokenByUserId(Long userId) {
        Date expireDate = new Date(new Date().getTime() + EXPIRES_IN);
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256).
                compact();
    }

    /**
     * @throws JwtException if the token is malformed, expired or signed with another key.
     *                      Raised by parseClaimsJws -- call validateToken first.
     */
    Long getUserIdFromJWT(String token){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key) // pass SecretKey, not raw string
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key) // use SecretKey, not plain String
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key) // use SecretKey, not plain String
                .build()
                .parseClaimsJws(token).getBody().getExpiration();
        return expiration.before(new Date());
    }
}
