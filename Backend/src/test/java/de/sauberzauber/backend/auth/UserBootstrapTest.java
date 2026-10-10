package de.sauberzauber.backend.auth;

import java.util.List;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserBootstrapTest {

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private EnrollmentService enrollment;

	@Autowired
	private PublicKeyCredentialUserEntityRepository userEntities;

	@Autowired
	private UserCredentialRepository userCredentials;

	@BeforeEach
	void reset() {
		this.jdbc.sql("delete from enrollment_token").update();
		this.jdbc.sql("delete from user_credentials").update();
		this.jdbc.sql("delete from user_entities").update();
	}

	@Test
	void runningTwiceCreatesEachUserOnce() {
		bootstrap(null).run(new DefaultApplicationArguments());
		bootstrap(null).run(new DefaultApplicationArguments());

		assertThat(this.jdbc.sql("select username from app_user order by username").query(String.class).list())
			.containsExactly("florian", "ronja");
		assertThat(tokenUsers()).containsExactly("florian", "ronja");
	}

	@Test
	void userWithPasskeyGetsNoEnrollmentToken() {
		addPasskey("florian");

		bootstrap(null).run(new DefaultApplicationArguments());

		assertThat(tokenUsers()).containsExactly("ronja");
	}

	@Test
	void reenrollForcesTokenForUserWithPasskey() {
		addPasskey("florian");

		bootstrap("florian").run(new DefaultApplicationArguments());

		assertThat(tokenUsers()).containsExactly("florian", "ronja");
	}

	private UserBootstrap bootstrap(String reenroll) {
		AppProperties props = new AppProperties(List.of("florian", "ronja"), "localhost", "http://localhost:8080",
				reenroll);
		return new UserBootstrap(props, this.jdbc, this.enrollment, this.userEntities, this.userCredentials);
	}

	private List<String> tokenUsers() {
		return this.jdbc.sql("select username from enrollment_token order by username").query(String.class).list();
	}

	private void addPasskey(String username) {
		this.jdbc.sql("insert into user_entities (id, name, display_name) values ('AAAA', ?, ?)")
			.params(username, username)
			.update();
		this.jdbc.sql("""
				insert into user_credentials (credential_id, user_entity_user_id, public_key, signature_count,
				  uv_initialized, backup_eligible, authenticator_transports, public_key_credential_type, backup_state,
				  created, last_used, label)
				values ('AQID', 'AAAA', '\\x00', 0, true, false, '', 'public-key', false, now(), now(), 'Test')
				""").update();
	}

}
