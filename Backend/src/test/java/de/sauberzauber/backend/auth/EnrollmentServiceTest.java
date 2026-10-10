package de.sauberzauber.backend.auth;

import java.util.Base64;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class EnrollmentServiceTest {

	@Autowired
	private EnrollmentService enrollment;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void deleteTokens() {
		this.jdbc.sql("delete from enrollment_token").update();
	}

	@Test
	void issueStoresOnlyTheHash() {
		String token = this.enrollment.issue("florian");

		assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
		byte[] stored = this.jdbc.sql("select token_hash from enrollment_token where username = 'florian'")
			.query(byte[].class)
			.single();
		assertThat(stored).isEqualTo(EnrollmentService.hash(token)).isNotEqualTo(token.getBytes());
	}

	@Test
	void validTokenCanBeConsumedOnlyOnce() {
		String token = this.enrollment.issue("florian");

		assertThat(this.enrollment.consume(token)).contains("florian");
		assertThat(this.enrollment.consume(token)).isEmpty();
	}

	@Test
	void expiredTokenIsRejected() {
		String token = this.enrollment.issue("florian");
		this.jdbc.sql("update enrollment_token set expires_at = now() - interval '1 second'").update();

		assertThat(this.enrollment.consume(token)).isEmpty();
	}

	@Test
	void issuingAgainInvalidatesTheOldToken() {
		String oldToken = this.enrollment.issue("florian");
		String newToken = this.enrollment.issue("florian");

		assertThat(this.enrollment.consume(oldToken)).isEmpty();
		assertThat(this.enrollment.consume(newToken)).contains("florian");
	}

	@Test
	void unknownOrMissingTokenIsRejected() {
		this.enrollment.issue("florian");

		assertThat(this.enrollment.consume("unknown")).isEmpty();
		assertThat(this.enrollment.consume(null)).isEmpty();
	}

}
