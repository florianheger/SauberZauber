package de.sauberzauber.backend.task;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A task's fields. On update, {@code counts} (by username) optionally corrects how often each
 * user did the task; {@code null} leaves the completions unchanged.
 */
public record TaskRequest(@NotBlank @Size(max = 100) String title, @Size(max = 500) String description,
		@NotNull @Pattern(regexp = "[a-z0-9_]{1,64}") String icon,
		Map<String, @NotNull @PositiveOrZero @Max(10000) Integer> counts) {

	public TaskRequest {
		title = (title != null) ? title.trim() : null;
		description = (description != null && !description.isBlank()) ? description.trim() : null;
	}

}
