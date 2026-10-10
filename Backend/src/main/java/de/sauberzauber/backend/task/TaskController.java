package de.sauberzauber.backend.task;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
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

	public TaskController(TaskRepository tasks) {
		this.tasks = tasks;
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
		if (!this.tasks.update(id, request)) {
			throw notFound();
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
	public TaskRepository.Completion complete(@PathVariable long id, @Valid @RequestBody CompletionRequest request) {
		if (!this.tasks.exists(id)) {
			throw notFound();
		}
		return this.tasks.addCompletion(id, request.username())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user"));
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

}
