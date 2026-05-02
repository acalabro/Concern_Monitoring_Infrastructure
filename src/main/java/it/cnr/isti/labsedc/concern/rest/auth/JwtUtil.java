package it.cnr.isti.labsedc.concern.rest.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

public class JwtUtil {

    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000L; // 8 hours

    private static final SecretKey KEY;

    static {
        String secret = System.getenv().getOrDefault("JWT_SECRET", "concern-monitoring-default-secret-key");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            KEY = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
