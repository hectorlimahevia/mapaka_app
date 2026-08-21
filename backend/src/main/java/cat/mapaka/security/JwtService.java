package cat.mapaka.security;

import cat.mapaka.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Emissió i validació de JWT (Família+.pdf secció 40): access token curt, refresh token més llarg,
 * mateixa clau de signatura per als dos, distingits pel claim "purpose".
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public JwtService(
            @Value("${mapaka.jwt.secret}") String secret,
            @Value("${mapaka.jwt.access-expiration}") Duration accessExpiration,
            @Value("${mapaka.jwt.refresh-expiration}") Duration refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(AuthenticatedUser user) {
        var builder = Jwts.builder()
                .subject(user.userId().toString())
                .claim("purpose", "access")
                .claim("role", user.role().name())
                .claim("familyId", user.familyId().toString())
                .issuedAt(new Date())
                .expiration(Date.from(new Date().toInstant().plus(accessExpiration)))
                .signWith(key);
        if (user.childId() != null) {
            builder.claim("childId", user.childId().toString());
        }
        return builder.compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", "refresh")
                .issuedAt(new Date())
                .expiration(Date.from(new Date().toInstant().plus(refreshExpiration)))
                .signWith(key)
                .compact();
    }

    public Duration refreshExpiration() {
        return refreshExpiration;
    }

    /** Token de curta durada emès per POST /api/auth/recover — només permet el reset de PIN
     * del primer PARENT de la família (mapaka_prompts_code.md Prompt 6). */
    public String generateRecoveryToken(UUID userId, UUID familyId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", "recovery")
                .claim("familyId", familyId.toString())
                .issuedAt(new Date())
                .expiration(Date.from(new Date().toInstant().plus(Duration.ofMinutes(10))))
                .signWith(key)
                .compact();
    }

    public RecoveryClaims parseRecoveryToken(String token) {
        Claims claims = parseClaims(token);
        if (!"recovery".equals(claims.get("purpose", String.class))) {
            throw new io.jsonwebtoken.JwtException("Token no és un token de recuperació");
        }
        return new RecoveryClaims(UUID.fromString(claims.getSubject()), UUID.fromString(claims.get("familyId", String.class)));
    }

    public record RecoveryClaims(UUID userId, UUID familyId) {
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"access".equals(claims.get("purpose", String.class))) {
            throw new io.jsonwebtoken.JwtException("Token no és un access token");
        }
        UUID childId = claims.get("childId", String.class) != null
                ? UUID.fromString(claims.get("childId", String.class))
                : null;
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("familyId", String.class)),
                UserRole.valueOf(claims.get("role", String.class)),
                childId);
    }

    public UUID parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"refresh".equals(claims.get("purpose", String.class))) {
            throw new io.jsonwebtoken.JwtException("Token no és un refresh token");
        }
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        return jws.getPayload();
    }
}
