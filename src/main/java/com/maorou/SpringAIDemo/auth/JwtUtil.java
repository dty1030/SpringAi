package com.maorou.SpringAIDemo.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    public static String createToken(
            String secret,
            long ttlMillis,
            String userId,
            String role) {
        SecretKey key = buildKey(secret);
        Date expiration = new Date(System.currentTimeMillis() + ttlMillis);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public static CurrentUser parseToken(String secret, String token) {
        SecretKey key = buildKey(secret);
        var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.get(CLAIM_USER_ID, String.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        return new CurrentUser(userId, role);
    }

    private static SecretKey buildKey(String secret) {
        // Use the configured secret for both signing and verifying JWTs.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
