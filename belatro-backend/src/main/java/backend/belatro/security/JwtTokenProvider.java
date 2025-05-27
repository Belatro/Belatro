package backend.belatro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key key;

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // derive a secure key from your secret string
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String createToken(String subject, long expirationMinutes) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    public void invalidateToken(String token) {
        revokedTokens.put(token, getExpiration(token).getTime());
    }

    public boolean isRevoked(String token) {
        return revokedTokens.containsKey(token);
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }


    public String getSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    public String getUsername(String token) {
        return getSubject(token);
    }

    public boolean validateToken(String token) {
        try {
            // 1) not revoked
            if (isRevoked(token)) return false;

            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Scheduled(fixedRate = 60_000)
    private void purgeExpiredRevokedTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(e -> e.getValue() < now);
    }



}
