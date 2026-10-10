package de.sauberzauber.backend.user;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The two household members, in the order they were created. */
@RestController
@RequestMapping("/api")
public class UserController {

	public record UserResponse(String username, String displayName) {
	}

	private final JdbcClient jdbc;

	public UserController(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping("/users")
	public List<UserResponse> users() {
		return this.jdbc.sql("select username from app_user order by id")
			.query(String.class)
			.list()
			.stream()
			.map((username) -> new UserResponse(username, displayName(username)))
			.toList();
	}

	static String displayName(String username) {
		return username.substring(0, 1).toUpperCase() + username.substring(1);
	}

}
