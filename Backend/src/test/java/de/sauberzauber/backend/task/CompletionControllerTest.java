package de.sauberzauber.backend.task;

import com.jayway.jsonpath.JsonPath;
import de.sauberzauber.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CompletionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcClient jdbc;

	private long taskId;

	@BeforeEach
	void createTask() {
		this.jdbc.sql("delete from task").update();
		this.taskId = this.jdbc.sql("insert into task (title, icon) values ('Müll rausbringen', 'delete') returning id")
			.query(Long.class)
			.single();
	}

	@Test
	void markDoneForMyselfAndForTheOtherUser() throws Exception {
		complete("alex").andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("alex"))
			.andExpect(jsonPath("$.at").value(notNullValue()));
		complete("alex").andExpect(status().isCreated());
		complete("sam").andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/tasks").with(user("alex")))
			.andExpect(jsonPath("$[0].counts.alex").value(2))
			.andExpect(jsonPath("$[0].counts.sam").value(1))
			.andExpect(jsonPath("$[0].lastDone.username").value("sam"))
			.andExpect(jsonPath("$[0].lastDone.at").value(notNullValue()));
	}

	@Test
	void unknownUserIsRejected() throws Exception {
		complete("mallory").andExpect(status().isBadRequest());
	}

	@Test
	void unknownTaskIsNotFound() throws Exception {
		this.mockMvc.perform(post("/api/tasks/999999/completions").with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"alex\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void undoRemovesTheCompletion() throws Exception {
		long completionId = ((Number) JsonPath.read(
				complete("alex").andReturn().getResponse().getContentAsString(), "$.id"))
			.longValue();

		this.mockMvc.perform(delete("/api/completions/" + completionId).with(user("alex")).with(csrf()))
			.andExpect(status().isNoContent());
		this.mockMvc.perform(delete("/api/completions/" + completionId).with(user("alex")).with(csrf()))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get("/api/tasks").with(user("alex"))).andExpect(jsonPath("$[0].counts.alex").value(0));
	}

	@Test
	void deletingTheTaskDeletesItsCompletions() throws Exception {
		complete("alex");
		complete("sam");

		this.mockMvc.perform(delete("/api/tasks/" + this.taskId).with(user("alex")).with(csrf()))
			.andExpect(status().isNoContent());

		assertThat(this.jdbc.sql("select count(*) from completion").query(Long.class).single()).isZero();
	}

	private ResultActions complete(String username) throws Exception {
		return this.mockMvc.perform(post("/api/tasks/" + this.taskId + "/completions").with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"" + username + "\"}"));
	}

}
