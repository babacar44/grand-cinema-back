package fr.grandcinema.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Création et vérification des jetons JWT (connexion sans session côté serveur). */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        // HS256 exige une clé d'au moins 256 bits (32 caractères)
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String genererToken(String email, String role) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(cle)
                .compact();
    }

    /** Renvoie l'e-mail contenu dans le jeton, ou null si le jeton est invalide ou expiré. */
    public String extraireEmail(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
