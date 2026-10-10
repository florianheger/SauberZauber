package de.sauberzauber.backend.task;

import java.time.Instant;
import java.util.Map;

/**
 * A task with how often each user has done it ({@code counts} by username) and the most
 * recent completion.
 */
public record TaskResponse(long id, String title, String description, String icon, Map<String, Long> counts,
		LastDone lastDone) {

	public record LastDone(String username, Instant at) {
	}

}
