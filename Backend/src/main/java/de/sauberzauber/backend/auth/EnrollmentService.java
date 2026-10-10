package de.sauberzauber.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time enrollment tokens for registering a user's first passkey. Only the SHA-256
 * hash of a token is stored.
 */
@Service
public class EnrollmentService {

	static final Duration VALIDITY = Duration.ofHours(24);

	private final SecureRandom random = new SecureRandom();

	private final JdbcClient jdbc;

	public EnrollmentService(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Creates a new token for the user, replacing any previous one. */
	public String issue(String username) {
		byte[] bytes = new byte[32];
		this.random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		this.jdbc.sql("""
				insert into enrollment_token (username, token_hash, expires_at) values (?, ?, ?)
				on conflict (username) do update set token_hash = excluded.token_hash, expires_at = excluded.expires_at
				""")
			.params(username, hash(token), Timestamp.from(Instant.now().plus(VALIDITY)))
			.update();
		return token;
	}

	/** Validates and deletes the token; returns the user name if it was valid. */
	@Transactional
	public Optional<String> consume(String token) {
		if (token == null || token.isEmpty()) {
			return Optional.empty();
		}
		byte[] presented = hash(token);
		List<StoredToken> stored = this.jdbc.sql("select username, token_hash, expires_at from enrollment_token")
			.query((rs, i) -> new StoredToken(rs.getString(1), rs.getBytes(2), rs.getTimestamp(3).toInstant()))
			.list();
		for (StoredToken candidate : stored) {
			if (MessageDigest.isEqual(candidate.hash(), presented)) {
				int deleted = this.jdbc.sql("delete from enrollment_token where username = ? and token_hash = ?")
					.params(candidate.username(), candidate.hash())
					.update();
				boolean valid = deleted == 1 && candidate.expiresAt().isAfter(Instant.now());
				return valid ? Optional.of(candidate.username()) : Optional.empty();
			}
		}
		return Optional.empty();
	}

	static byte[] hash(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private record StoredToken(String username, byte[] hash, Instant expiresAt) {
	}

}
