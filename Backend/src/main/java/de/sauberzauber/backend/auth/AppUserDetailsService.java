package de.sauberzauber.backend.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads the users created from {@code APP_USERS}. Login only happens via passkey, so the
 * password is a placeholder without a matching encoder that can never be used.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final JdbcClient jdbc;

	public AppUserDetailsService(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		return this.jdbc.sql("select username from app_user where username = ?")
			.param(username)
			.query(String.class)
			.optional()
			.map((name) -> User.withUsername(name).password("{disabled}").roles("USER").build())
			.orElseThrow(() -> new UsernameNotFoundException(username));
	}

}
