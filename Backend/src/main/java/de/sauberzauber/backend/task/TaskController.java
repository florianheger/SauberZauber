package de.sauberzauber.backend.task;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import de.sauberzauber.backend.push.CompletionNotifier;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class TaskController {

	public record CompletionRequest(@NotBlank String username) {
	}

	private final TaskRepository tasks;

	private final CompletionNotifier notifier;

	public TaskController(TaskRepository tasks, CompletionNotifier notifier) {
		this.tasks = tasks;
		this.notifier = notifier;
	}

	@GetMapping("/tasks")
	public List<TaskResponse> list() {
		return this.tasks.findAll();
	}

	@PostMapping("/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(@Valid @RequestBody TaskRequest request) {
		long id = this.tasks.create(request);
		return find(id);
	}

	@PutMapping("/tasks/{id}")
	public TaskResponse update(@PathVariable long id, @Valid @RequestBody TaskRequest request) {
		switch (this.tasks.update(id, request)) {
			case NOT_FOUND -> throw notFound();
			case UNKNOWN_USER -> throw unknownUser();
			case UPDATED -> {
			}
		}
		return find(id);
	}

	@DeleteMapping("/tasks/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long id) {
		if (!this.tasks.delete(id)) {
			throw notFound();
		}
	}

	@PostMapping("/tasks/{id}/completions")
	@ResponseStatus(HttpStatus.CREATED)
	public TaskRepository.Completion complete(@PathVariable long id, Authentication authentication,
			@Valid @RequestBody CompletionRequest request) {
		String title = this.tasks.findTitle(id).orElseThrow(this::notFound);
		TaskRepository.Completion completion = this.tasks.addCompletion(id, request.username())
			.orElseThrow(this::unknownUser);
		this.notifier.completionAdded(authentication.getName(), request.username(), title, completion.id());
		return completion;
	}

	@DeleteMapping("/completions/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void undo(@PathVariable long id) {
		if (!this.tasks.deleteCompletion(id)) {
			throw notFound();
		}
	}

	private TaskResponse find(long id) {
		return this.tasks.findAll().stream().filter((task) -> task.id() == id).findFirst().orElseThrow(this::notFound);
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

	private ResponseStatusException unknownUser() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user");
	}

}
