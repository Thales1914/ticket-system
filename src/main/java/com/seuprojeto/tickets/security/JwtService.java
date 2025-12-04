package com.seuprojeto.tickets.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // -----------------------------------------------------------
    // 🔹 GERAR TOKEN COM ID E ROLE
    // -----------------------------------------------------------
    public String generateToken(Long userId, String role) {

        return Jwts.builder()
                .setSubject(String.valueOf(userId))  // ID do usuário
                .claim("role", role)                 // ROLE do usuário
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // -----------------------------------------------------------
    // 🔹 VALIDAR TOKEN
    // -----------------------------------------------------------
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------
    // 🔹 PEGAR USER ID DO TOKEN
    // -----------------------------------------------------------
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getAllClaims(token).getSubject());
    }

    // -----------------------------------------------------------
    // 🔹 PEGAR ROLE DO TOKEN
    // -----------------------------------------------------------
    public String getRoleFromToken(String token) {
        return getAllClaims(token).get("role", String.class);
    }

    // -----------------------------------------------------------
    // 🔹 PEGAR TODAS AS CLAIMS
    // -----------------------------------------------------------
    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
