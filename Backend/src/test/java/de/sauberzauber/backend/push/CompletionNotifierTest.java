package de.sauberzauber.backend.push;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.jayway.jsonpath.JsonPath;
import de.sauberzauber.backend.IntegrationTest;
import de.sauberzauber.backend.push.PushSubscriptionRepository.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CompletionNotifierTest {

	private static final Subscription ALEX_PHONE = new Subscription("https://push.example.com/alex", "a1", "a2");

	private static final Subscription SAM_PHONE = new Subscription("https://push.example.com/sam", "s1", "s2");

	@MockitoBean
	private PushSender sender;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PushSubscriptionRepository subscriptions;

	@Autowired
	private JdbcClient jdbc;

	private long taskId;

	@BeforeEach
	void setUp() {
		this.jdbc.sql("delete from push_subscription").update();
		this.jdbc.sql("delete from task").update();
		this.taskId = this.jdbc.sql("insert into task (title, icon) values ('Bad putzen', 'bathtub') returning id")
			.query(Long.class)
			.single();
		this.subscriptions.save("alex", ALEX_PHONE);
		this.subscriptions.save("sam", SAM_PHONE);
		when(this.sender.send(any(), anyString())).thenReturn(CompletableFuture.completedFuture(201));
	}

	@Test
	void doneForMyselfNotifiesTheOtherUser() throws Exception {
		complete("alex", "alex");

		assertThat(title(sentTo(SAM_PHONE))).isEqualTo("Alex hat Bad putzen erledigt");
		verify(this.sender, never()).send(eq(ALEX_PHONE), anyString());
	}

	@Test
	void doneForTheOtherUserNotifiesThem() throws Exception {
		complete("alex", "sam");

		assertThat(title(sentTo(SAM_PHONE))).isEqualTo("Alex hat Bad putzen für dich eingetragen");
		verify(this.sender, never()).send(eq(ALEX_PHONE), anyString());
	}

	@Test
	void payloadOpensTheApp() throws Exception {
		complete("sam", "sam");

		String payload = sentTo(ALEX_PHONE);
		assertThat(title(payload)).isEqualTo("Sam hat Bad putzen erledigt");
		assertThat((Integer) JsonPath.read(payload, "$.web_push")).isEqualTo(8030);
		assertThat((String) JsonPath.read(payload, "$.notification.navigate")).isEqualTo("http://localhost:8080/");
		assertThat((String) JsonPath.read(payload, "$.notification.data.onActionClick.default.operation"))
			.isEqualTo("navigateLastFocusedOrOpen");
	}

	@Test
	void undoAndCountCorrectionsDoNotNotify() throws Exception {
		String response = complete("alex", "alex").andReturn().getResponse().getContentAsString();
		clearInvocations(this.sender);

		this.mockMvc.perform(delete("/api/completions/" + JsonPath.read(response, "$.id")).with(user("alex"))
			.with(csrf())).andExpect(status().isNoContent());
		this.mockMvc.perform(put("/api/tasks/" + this.taskId).with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\": \"Bad putzen\", \"icon\": \"bathtub\", \"counts\": {\"alex\": 3, \"sam\": 2}}"))
			.andExpect(status().isOk());

		verify(this.sender, never()).send(any(), anyString());
	}

	@Test
	void goneSubscriptionsAreDeleted() throws Exception {
		when(this.sender.send(any(), anyString())).thenReturn(CompletableFuture.completedFuture(410));

		complete("alex", "alex");

		assertThat(this.subscriptions.findByUsername("sam")).isEmpty();
		assertThat(this.subscriptions.findByUsername("alex")).containsExactly(ALEX_PHONE);
	}

	@Test
	void failingPushServiceDoesNotFailTheCompletion() throws Exception {
		when(this.sender.send(any(), anyString()))
			.thenReturn(CompletableFuture.failedFuture(new IOException("unreachable")));

		complete("alex", "alex");

		assertThat(this.subscriptions.findByUsername("sam")).containsExactly(SAM_PHONE);
	}

	private ResultActions complete(String actor, String doer) throws Exception {
		return this.mockMvc
			.perform(post("/api/tasks/" + this.taskId + "/completions").with(user(actor))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\": \"" + doer + "\"}"))
			.andExpect(status().isCreated());
	}

	private String sentTo(Subscription subscription) {
		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(this.sender).send(eq(subscription), payload.capture());
		return payload.getValue();
	}

	private static String title(String payload) {
		return JsonPath.read(payload, "$.notification.title");
	}

}
