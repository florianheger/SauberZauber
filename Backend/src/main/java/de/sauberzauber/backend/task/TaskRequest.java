package de.sauberzauber.backend.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskRequest(@NotBlank @Size(max = 100) String title, @Size(max = 500) String description,
		@NotNull @Pattern(regexp = "[a-z0-9_]{1,64}") String icon) {

	public TaskRequest {
		title = (title != null) ? title.trim() : null;
		description = (description != null && !description.isBlank()) ? description.trim() : null;
	}

}
