package com.mentora.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String encodedSecret,
            @Value("${security.jwt.expiration-ms}") long expirationMillis
    ) {
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero");
        }
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token subject is missing");
        }
        if (subject.length() > 254) {
            throw new JwtException("Token subject is invalid");
        }
        return subject;
    }
}
