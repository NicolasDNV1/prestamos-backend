package com.alimentared.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiracion;

    public JwtService(AlimentaRedProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
        this.expiracion = Duration.ofMinutes(properties.jwt().expiracionMinutos());
    }

    public String generarToken(UsuarioPrincipal principal) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("rol", principal.getRol().name())
                .claim("org", principal.getOrganizacionId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(key)
                .compact();
    }

    /** Devuelve el correo (subject) si la firma y la expiración son válidas. */
    public Optional<String> extraerCorreoSiValido(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpiracionSegundos() {
        return expiracion.toSeconds();
    }
}
