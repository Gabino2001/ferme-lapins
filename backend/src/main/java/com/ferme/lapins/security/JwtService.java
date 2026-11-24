package com.ferme.lapins.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {
    private final SecretKey key;
    private final long hours;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // 32 caractères minimum
        this.hours = hours;
    }

    public String generer(Long id, String email) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(id)).claim("email", email)
                .issuedAt(now).expiration(new Date(now.getTime() + hours * 3_600_000L))
                .signWith(key).compact();
    }

    /** Jeton très court, valable seulement pour télécharger un export (placé dans un lien). */
    public String genererExport(Long id, int minutes) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(id)).claim("usage", "export")
                .issuedAt(now).expiration(new Date(now.getTime() + minutes * 60_000L))
                .signWith(key).compact();
    }

    private Claims lireClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** Jeton de connexion normal. Un jeton d'export n'est pas accepté pour l'API. */
    public Long lire(String token) {
        Claims c = lireClaims(token);
        if (c.get("usage") != null) throw new JwtException("Jeton d’export non valable pour l’API");
        return Long.valueOf(c.getSubject());
    }

    public Long lireExport(String token) {
        Claims c = lireClaims(token);
        if (!"export".equals(c.get("usage"))) throw new JwtException("Jeton d’export invalide");
        return Long.valueOf(c.getSubject());
    }
}
