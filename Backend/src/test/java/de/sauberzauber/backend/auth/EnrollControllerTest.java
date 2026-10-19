package de.sauberzauber.backend.auth;

import jakarta.servlet.http.Cookie;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class EnrollControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EnrollmentService enrollment;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void deleteTokens() {
		this.jdbc.sql("delete from enrollment_token").update();
	}

	@Test
	void validTokenAuthenticatesAndRedirectsToRegistration() throws Exception {
		String token = this.enrollment.issue("alex");

		Cookie session = this.mockMvc.perform(get("/enroll").param("token", token))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/webauthn/register"))
			.andReturn()
			.getResponse()
			.getCookie("SESSION");

		assertThat(session).isNotNull();
		this.mockMvc.perform(get("/api/me").cookie(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("alex"));
	}

	@Test
	void enrollmentChangesTheSessionId() throws Exception {
		Cookie anonymous = this.mockMvc.perform(get("/webauthn/register"))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getCookie("SESSION");
		assertThat(anonymous).isNotNull();

		Cookie session = this.mockMvc.perform(get("/enroll").param("token", this.enrollment.issue("alex")).cookie(anonymous))
			.andExpect(status().isFound())
			.andReturn()
			.getResponse()
			.getCookie("SESSION");

		assertThat(session.getValue()).isNotEqualTo(anonymous.getValue());
		this.mockMvc.perform(get("/api/me").cookie(anonymous)).andExpect(status().isUnauthorized());
	}

	@Test
	void reusedTokenIsForbidden() throws Exception {
		String token = this.enrollment.issue("alex");
		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isFound());

		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isForbidden());
	}

	@Test
	void expiredTokenIsForbidden() throws Exception {
		String token = this.enrollment.issue("alex");
		this.jdbc.sql("update enrollment_token set expires_at = now() - interval '1 second'").update();

		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isForbidden());
	}

	@Test
	void invalidOrMissingTokenIsForbidden() throws Exception {
		this.mockMvc.perform(get("/enroll").param("token", "invalid")).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/enroll")).andExpect(status().isForbidden());
	}

}
