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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void deleteTasks() {
		this.jdbc.sql("delete from task").update();
	}

	@Test
	void createListUpdateDelete() throws Exception {
		long id = create("""
				{"title": "  Wäsche waschen ", "description": " ", "icon": "local_laundry_service"}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("Wäsche waschen"))
			.andExpect(jsonPath("$.description").value(nullValue()))
			.andExpect(jsonPath("$.counts.alex").value(0))
			.andExpect(jsonPath("$.counts.sam").value(0))
			.andExpect(jsonPath("$.lastDone").value(nullValue()))
			.andReturn()
			.getResponse()
			.getContentAsString()
			.transform((json) -> ((Number) JsonPath.read(json, "$.id")).longValue());

		this.mockMvc.perform(put("/api/tasks/" + id).with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Wäsche", "description": "Inklusive Aufhängen", "icon": "dry_cleaning"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.description").value("Inklusive Aufhängen"))
			.andExpect(jsonPath("$.icon").value("dry_cleaning"));

		this.mockMvc.perform(get("/api/tasks").with(user("alex")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].title").value("Wäsche"));

		this.mockMvc.perform(delete("/api/tasks/" + id).with(user("alex")).with(csrf()))
			.andExpect(status().isNoContent());
		this.mockMvc.perform(get("/api/tasks").with(user("alex"))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void invalidInputIsRejected() throws Exception {
		create("""
				{"title": "   ", "icon": "delete"}""").andExpect(status().isBadRequest());
		create("""
				{"title": "Müll", "icon": "Not An Icon!"}""").andExpect(status().isBadRequest());
		create("""
				{"title": "Müll"}""").andExpect(status().isBadRequest());
		create("{\"title\": \"" + "x".repeat(101) + "\", \"icon\": \"delete\"}").andExpect(status().isBadRequest());
	}

	@Test
	void updateCorrectsCounts() throws Exception {
		long id = createId();
		addCompletion(id, "alex", "2026-01-01T10:00:00Z");
		addCompletion(id, "alex", "2026-01-02T10:00:00Z");
		addCompletion(id, "sam", "2026-01-01T12:00:00Z");

		update(id, """
				{"title": "Müll", "icon": "delete", "counts": {"alex": 1}}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.counts.alex").value(1))
			.andExpect(jsonPath("$.counts.sam").value(1))
			.andExpect(jsonPath("$.lastDone.username").value("sam"));

		update(id, """
				{"title": "Müll", "icon": "delete", "counts": {"alex": 1, "sam": 3}}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.counts.alex").value(1))
			.andExpect(jsonPath("$.counts.sam").value(3));

		update(id, """
				{"title": "Müll", "icon": "delete"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.counts.alex").value(1))
			.andExpect(jsonPath("$.counts.sam").value(3));

		update(id, """
				{"title": "Müll", "icon": "delete", "counts": {"alex": 0, "sam": 0}}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.counts.alex").value(0))
			.andExpect(jsonPath("$.lastDone").value(nullValue()));
	}

	@Test
	void invalidCountsAreRejected() throws Exception {
		long id = createId();
		addCompletion(id, "alex", "2026-01-01T10:00:00Z");

		update(id, """
				{"title": "Müll", "icon": "delete", "counts": {"alex": -1}}""").andExpect(status().isBadRequest());
		update(id, """
				{"title": "Müll", "icon": "delete", "counts": {"alex": 100000}}""").andExpect(status().isBadRequest());
		update(id, """
				{"title": "Geändert", "icon": "delete", "counts": {"alex": 0, "nobody": 1}}""")
			.andExpect(status().isBadRequest());

		this.mockMvc.perform(get("/api/tasks").with(user("alex")))
			.andExpect(jsonPath("$[0].title").value("Müll"))
			.andExpect(jsonPath("$[0].counts.alex").value(1));
	}

	@Test
	void unknownTaskIsNotFound() throws Exception {
		this.mockMvc.perform(put("/api/tasks/999999").with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Müll", "icon": "delete"}"""))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(delete("/api/tasks/999999").with(user("alex")).with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void writesRequireCsrfToken() throws Exception {
		this.mockMvc.perform(post("/api/tasks").with(user("alex"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"title": "Müll", "icon": "delete"}"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void requiresAuthentication() throws Exception {
		this.mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
	}

	private long createId() throws Exception {
		return create("""
				{"title": "Müll", "icon": "delete"}""").andReturn()
			.getResponse()
			.getContentAsString()
			.transform((json) -> ((Number) JsonPath.read(json, "$.id")).longValue());
	}

	private void addCompletion(long taskId, String username, String at) {
		this.jdbc.sql("""
				insert into completion (task_id, user_id, done_at)
				select ?, id, ?::timestamptz from app_user where username = ?
				""").params(taskId, at, username).update();
	}

	private ResultActions update(long id, String json) throws Exception {
		return this.mockMvc.perform(put("/api/tasks/" + id).with(user("alex"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions create(String json) throws Exception {
		return this.mockMvc.perform(
				post("/api/tasks").with(user("alex")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
