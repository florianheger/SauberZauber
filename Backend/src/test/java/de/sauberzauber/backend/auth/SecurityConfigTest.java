package de.sauberzauber.backend.auth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiRequiresAuthentication() throws Exception {
		this.mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void httpBasicIsRejected() throws Exception {
		this.mockMvc.perform(get("/api/me").with(httpBasic("florian", "{disabled}")))
			.andExpect(status().isUnauthorized())
			.andExpect(unauthenticated());
	}

	@Test
	void formLoginIsRejected() throws Exception {
		this.mockMvc.perform(post("/login").param("username", "florian").param("password", "{disabled}").with(csrf()))
			.andExpect(status().isUnauthorized())
			.andExpect(unauthenticated());
	}

	@Test
	void meReturnsTheCurrentUser() throws Exception {
		this.mockMvc.perform(get("/api/me").with(user("florian")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("florian"));
	}

	@Test
	void logoutRequiresCsrfToken() throws Exception {
		this.mockMvc.perform(post("/logout").with(user("florian"))).andExpect(status().isForbidden());
		this.mockMvc.perform(post("/logout").with(user("florian")).with(csrf())).andExpect(status().isOk());
	}

	@Test
	void healthIsPublic() throws Exception {
		this.mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void registrationPageCsrfTokenIsAccepted() throws Exception {
		MvcResult page = this.mockMvc.perform(get("/webauthn/register").with(user("florian")))
			.andExpect(status().isOk())
			.andReturn();
		Matcher matcher = Pattern.compile("\"X-XSRF-TOKEN\" : \"([^\"]+)\"")
			.matcher(page.getResponse().getContentAsString());
		assertThat(matcher.find()).isTrue();
		Cookie cookie = page.getResponse().getCookie("XSRF-TOKEN");
		assertThat(cookie).isNotNull();

		this.mockMvc
			.perform(post("/webauthn/register/options").with(user("florian"))
				.cookie(cookie)
				.header("X-XSRF-TOKEN", matcher.group(1)))
			.andExpect(status().isOk());
	}

}
