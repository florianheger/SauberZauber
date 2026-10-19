package de.sauberzauber.backend.push;

import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PushControllerTest {

	private static final String ENDPOINT = "https://push.example.com/send/abc";

	private static final String SUBSCRIPTION = """
			{"endpoint": "%s", "expirationTime": null, "keys": {"p256dh": "BCVx", "auth": "BTBZ"}}
			""".formatted(ENDPOINT);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PushSubscriptionRepository subscriptions;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void deleteSubscriptions() {
		this.jdbc.sql("delete from push_subscription").update();
	}

	@Test
	void publicKeyIsTheConfiguredVapidKey() throws Exception {
		this.mockMvc.perform(get("/api/push/public-key").with(user("alex")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.publicKey").value(IntegrationTest.VAPID_PUBLIC_KEY));
	}

	@Test
	void subscribeStoresTheSubscriptionForTheCurrentUser() throws Exception {
		subscribe("alex").andExpect(status().isNoContent());

		assertThat(this.subscriptions.findByUsername("alex"))
			.containsExactly(new PushSubscriptionRepository.Subscription(ENDPOINT, "BCVx", "BTBZ"));
		assertThat(this.subscriptions.findByUsername("sam")).isEmpty();
	}

	@Test
	void subscribingAgainMovesTheDeviceToTheNewUser() throws Exception {
		subscribe("alex");
		subscribe("alex");
		subscribe("sam");

		assertThat(this.subscriptions.findByUsername("alex")).isEmpty();
		assertThat(this.subscriptions.findByUsername("sam")).hasSize(1);
	}

	@Test
	void unsubscribeDeletesOnlyTheOwnSubscription() throws Exception {
		subscribe("alex");

		unsubscribe("sam");
		assertThat(this.subscriptions.findByUsername("alex")).hasSize(1);

		unsubscribe("alex");
		assertThat(this.subscriptions.findByUsername("alex")).isEmpty();
	}

	@Test
	void endpointMustBeHttps() throws Exception {
		this.mockMvc.perform(post("/api/push/subscriptions").with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"endpoint\": \"http://localhost:8080/x\", \"keys\": {\"p256dh\": \"a\", \"auth\": \"b\"}}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresLoginAndCsrf() throws Exception {
		this.mockMvc.perform(get("/api/push/public-key")).andExpect(status().isUnauthorized());
		this.mockMvc
			.perform(post("/api/push/subscriptions").with(user("alex"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(SUBSCRIPTION))
			.andExpect(status().isForbidden());
	}

	private ResultActions subscribe(String username) throws Exception {
		return this.mockMvc.perform(post("/api/push/subscriptions").with(user(username))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(SUBSCRIPTION));
	}

	private void unsubscribe(String username) throws Exception {
		this.mockMvc
			.perform(delete("/api/push/subscriptions").with(user(username))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"endpoint\": \"" + ENDPOINT + "\"}"))
			.andExpect(status().isNoContent());
	}

}
