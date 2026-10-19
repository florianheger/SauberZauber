package de.sauberzauber.backend.auth;

import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import de.sauberzauber.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logins survive closing the app (persistent cookie) and backend restarts (sessions in
 * Postgres). Runs against the embedded server, because only there Spring Boot applies
 * {@code server.servlet.session.cookie.*} to the Spring Session cookie.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "app.users=alex,sam",
		"app.rp-id=localhost", "app.origin=http://localhost:8080", "app.reenroll=" })
@Import(TestcontainersConfiguration.class)
class PersistentSessionTest {

	private static final Duration NINETY_DAYS = Duration.ofDays(90);

	@LocalServerPort
	private int port;

	@Autowired
	private EnrollmentService enrollment;

	@Autowired
	private JdbcClient jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void sessionIsPersistentForNinetyDays() throws Exception {
		HttpResponse<String> enroll = send("/enroll?token=" + this.enrollment.issue("sam"), null);
		assertThat(enroll.statusCode()).isEqualTo(302);
		String header = enroll.headers()
			.allValues("Set-Cookie")
			.stream()
			.filter((value) -> value.startsWith("SESSION="))
			.findFirst()
			.orElseThrow();
		assertThat(header).contains("SameSite=Lax");
		HttpCookie session = HttpCookie.parse(header).getFirst();
		assertThat(session.getMaxAge()).isEqualTo(NINETY_DAYS.toSeconds());
		assertThat(session.isHttpOnly()).isTrue();

		Integer maxInactive = this.jdbc
			.sql("select max_inactive_interval from spring_session where principal_name = 'sam'")
			.query(Integer.class)
			.single();
		assertThat(maxInactive).isEqualTo(NINETY_DAYS.toSeconds());

		HttpResponse<String> me = send("/api/me", session);
		assertThat(me.statusCode()).isEqualTo(200);
		assertThat(me.body()).contains("\"sam\"");
	}

	private HttpResponse<String> send(String path, HttpCookie session) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path));
		if (session != null) {
			request.header("Cookie", session.getName() + "=" + session.getValue());
		}
		return this.http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

}
