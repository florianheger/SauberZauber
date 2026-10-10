package de.sauberzauber.backend.task;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TaskRepository {

	public record Completion(long id, String username, Instant at) {
	}

	private record TaskRow(long id, String title, String description, String icon) {
	}

	private final JdbcClient jdbc;

	public TaskRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> findAll() {
		List<TaskRow> tasks = this.jdbc.sql("select id, title, description, icon from task order by created_at, id")
			.query(TaskRow.class)
			.list();
		List<String> usernames = this.jdbc.sql("select username from app_user order by id").query(String.class).list();
		Map<Long, Map<String, Long>> counts = new HashMap<>();
		this.jdbc.sql("""
				select c.task_id, u.username, count(*) from completion c join app_user u on u.id = c.user_id
				group by c.task_id, u.username
				""")
			.query((rs) -> {
				counts.computeIfAbsent(rs.getLong(1), (id) -> new HashMap<>()).put(rs.getString(2), rs.getLong(3));
			});
		Map<Long, TaskResponse.LastDone> lastDone = new HashMap<>();
		this.jdbc.sql("""
				select distinct on (c.task_id) c.task_id, u.username, c.done_at
				from completion c join app_user u on u.id = c.user_id
				order by c.task_id, c.done_at desc, c.id desc
				""")
			.query((rs) -> {
				lastDone.put(rs.getLong(1),
						new TaskResponse.LastDone(rs.getString(2), rs.getTimestamp(3).toInstant()));
			});
		return tasks.stream().map((task) -> {
			Map<String, Long> taskCounts = new LinkedHashMap<>();
			Map<String, Long> stored = counts.getOrDefault(task.id(), Map.of());
			usernames.forEach((username) -> taskCounts.put(username, stored.getOrDefault(username, 0L)));
			return new TaskResponse(task.id(), task.title(), task.description(), task.icon(), taskCounts,
					lastDone.get(task.id()));
		}).toList();
	}

	public long create(TaskRequest request) {
		return this.jdbc.sql("insert into task (title, description, icon) values (?, ?, ?) returning id")
			.params(request.title(), request.description(), request.icon())
			.query(Long.class)
			.single();
	}

	public boolean update(long id, TaskRequest request) {
		return this.jdbc.sql("update task set title = ?, description = ?, icon = ? where id = ?")
			.params(request.title(), request.description(), request.icon(), id)
			.update() == 1;
	}

	public boolean delete(long id) {
		return this.jdbc.sql("delete from task where id = ?").param(id).update() == 1;
	}

	public boolean exists(long id) {
		return this.jdbc.sql("select count(*) from task where id = ?").param(id).query(Long.class).single() > 0;
	}

	/** Records a completion; empty if the user does not exist. */
	public Optional<Completion> addCompletion(long taskId, String username) {
		return this.jdbc.sql("""
				insert into completion (task_id, user_id)
				select ?, id from app_user where username = ?
				returning id, done_at
				""")
			.params(taskId, username)
			.query((rs, i) -> new Completion(rs.getLong(1), username, rs.getTimestamp(2).toInstant()))
			.optional();
	}

	public boolean deleteCompletion(long id) {
		return this.jdbc.sql("delete from completion where id = ?").param(id).update() == 1;
	}

}
