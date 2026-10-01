package mg.improve.todo.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

	private final SecretKey signingKey;

	private final long accessExpiration;

	private final long refreshExpiration;

	public JwtService(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration.access}") long accessExpiration,
			@Value("${jwt.expiration.refresh}") long refreshExpiration
		) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
		this.accessExpiration = accessExpiration;
		this.refreshExpiration = refreshExpiration;
	}

	public boolean verifyToken(String token) {
		try {
			parse(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public String extractSubject(String token) {
		return parse(token).getSubject();
	}

	public Object extractClaim(String token, String claimName) {
		return parse(token).get(claimName);
	}

	public long getAccessExpiration() {
		return accessExpiration;
	}

	public long getRefreshExpiration() {
		return refreshExpiration;
	}

	public String createToken(UUID userId, boolean isAccessToken,  Map<String, Object> claims) {
		Date now = new Date();
		long milliToExpiration = isAccessToken ? accessExpiration : refreshExpiration;
		Date expiration = new Date(now.getTime() + milliToExpiration);

		return Jwts.builder()
				.claims(claims)
				.subject(userId.toString())
				.issuedAt(now)
				.expiration(expiration)
				.signWith(signingKey)
				.compact();
	}

	private Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
