package de.sauberzauber.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;

/**
 * Creates the users from {@code APP_USERS} and logs an enrollment link for every user
 * without a passkey (or the one named in {@code APP_REENROLL}).
 */
@Component
public class UserBootstrap implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(UserBootstrap.class);

	private final AppProperties props;

	private final JdbcClient jdbc;

	private final EnrollmentService enrollment;

	private final PublicKeyCredentialUserEntityRepository userEntities;

	private final UserCredentialRepository userCredentials;

	public UserBootstrap(AppProperties props, JdbcClient jdbc, EnrollmentService enrollment,
			PublicKeyCredentialUserEntityRepository userEntities, UserCredentialRepository userCredentials) {
		this.props = props;
		this.jdbc = jdbc;
		this.enrollment = enrollment;
		this.userEntities = userEntities;
		this.userCredentials = userCredentials;
	}

	@Override
	public void run(ApplicationArguments args) {
		for (String username : this.props.users()) {
			this.jdbc.sql("insert into app_user (username) values (?) on conflict (username) do nothing")
				.param(username)
				.update();
			if (!hasCredential(username) || username.equals(this.props.reenroll())) {
				String token = this.enrollment.issue(username);
				logger.info("Enrollment for {}: {}/enroll?token={}", username, this.props.origin(), token);
			}
		}
	}

	private boolean hasCredential(String username) {
		PublicKeyCredentialUserEntity entity = this.userEntities.findByUsername(username);
		return entity != null && !this.userCredentials.findByUserId(entity.getId()).isEmpty();
	}

}
