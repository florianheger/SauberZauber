package de.sauberzauber.backend.auth;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
		String token = this.enrollment.issue("florian");
		MockHttpSession session = new MockHttpSession();
		String originalSessionId = session.getId();

		MvcResult result = this.mockMvc.perform(get("/enroll").param("token", token).session(session))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/webauthn/register"))
			.andReturn();

		MockHttpSession resultSession = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(resultSession.getId()).isNotEqualTo(originalSessionId);
		SecurityContext context = (SecurityContext) resultSession
			.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertThat(context.getAuthentication().isAuthenticated()).isTrue();
		assertThat(context.getAuthentication().getName()).isEqualTo("florian");
	}

	@Test
	void reusedTokenIsForbidden() throws Exception {
		String token = this.enrollment.issue("florian");
		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isFound());

		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isForbidden());
	}

	@Test
	void expiredTokenIsForbidden() throws Exception {
		String token = this.enrollment.issue("florian");
		this.jdbc.sql("update enrollment_token set expires_at = now() - interval '1 second'").update();

		this.mockMvc.perform(get("/enroll").param("token", token)).andExpect(status().isForbidden());
	}

	@Test
	void invalidOrMissingTokenIsForbidden() throws Exception {
		this.mockMvc.perform(get("/enroll").param("token", "invalid")).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/enroll")).andExpect(status().isForbidden());
	}

}
