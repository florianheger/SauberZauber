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

	public enum UpdateResult {

		UPDATED, NOT_FOUND, UNKNOWN_USER

	}

	/** Updates the task and, if {@code counts} is given, corrects its completions to match. */
	@Transactional
	public UpdateResult update(long id, TaskRequest request) {
		Map<Long, Integer> targets = new HashMap<>();
		if (request.counts() != null) {
			for (Map.Entry<String, Integer> entry : request.counts().entrySet()) {
				Optional<Long> userId = this.jdbc.sql("select id from app_user where username = ?")
					.param(entry.getKey())
					.query(Long.class)
					.optional();
				if (userId.isEmpty()) {
					return UpdateResult.UNKNOWN_USER;
				}
				targets.put(userId.get(), entry.getValue());
			}
		}
		if (this.jdbc.sql("update task set title = ?, description = ?, icon = ? where id = ?")
			.params(request.title(), request.description(), request.icon(), id)
			.update() != 1) {
			return UpdateResult.NOT_FOUND;
		}
		targets.forEach((userId, target) -> setCount(id, userId, target));
		return UpdateResult.UPDATED;
	}

	/** Adds completions (done now) or removes the most recent ones until the user has {@code target}. */
	private void setCount(long taskId, long userId, int target) {
		long current = this.jdbc.sql("select count(*) from completion where task_id = ? and user_id = ?")
			.params(taskId, userId)
			.query(Long.class)
			.single();
		if (target > current) {
			this.jdbc.sql("insert into completion (task_id, user_id) select ?, ? from generate_series(1, ?)")
				.params(taskId, userId, (int) (target - current))
				.update();
		}
		else if (target < current) {
			this.jdbc.sql("""
					delete from completion where id in (
						select id from completion where task_id = ? and user_id = ?
						order by done_at desc, id desc limit ?)
					""")
				.params(taskId, userId, (int) (current - target))
				.update();
		}
	}

	public boolean delete(long id) {
		return this.jdbc.sql("delete from task where id = ?").param(id).update() == 1;
	}

	public Optional<String> findTitle(long id) {
		return this.jdbc.sql("select title from task where id = ?").param(id).query(String.class).optional();
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
