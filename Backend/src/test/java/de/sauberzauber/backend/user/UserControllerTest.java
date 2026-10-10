package de.sauberzauber.backend.user;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listsBothUsersInCreationOrderWithDisplayNames() throws Exception {
		this.mockMvc.perform(get("/api/users").with(user("alex")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].username").value("alex"))
			.andExpect(jsonPath("$[0].displayName").value("Alex"))
			.andExpect(jsonPath("$[1].username").value("sam"))
			.andExpect(jsonPath("$[1].displayName").value("Sam"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		this.mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
	}

}
