package mg.improve.todo.endpoint.rest;

import java.util.UUID;

import mg.improve.todo.domain.dto.TodoCreateRequest;
import mg.improve.todo.domain.dto.TodoPage;
import mg.improve.todo.domain.dto.TodoResponse;
import mg.improve.todo.domain.dto.TodoUpdateRequest;
import mg.improve.todo.service.TodoService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
public class TodoController {

	private final TodoService todoService;

	public TodoController(TodoService todoService) {
		this.todoService = todoService;
	}

	@GetMapping
	public TodoPage listTodos(
			@AuthenticationPrincipal UUID userId,
			@RequestParam(value = "isDone", required = false) Boolean isDone,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "perPage", defaultValue = "20") int perPage) {
		return todoService.listTodos(userId, isDone, title, page, perPage);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TodoResponse createTodo(
			@AuthenticationPrincipal UUID userId,
			@RequestBody TodoCreateRequest request) {
		return TodoResponse.from(todoService.createTodo(userId, request));
	}

	@GetMapping("/{todoId}")
	public TodoResponse getTodo(
			@AuthenticationPrincipal UUID userId, @PathVariable("todoId") UUID todoId) {
		return TodoResponse.from(todoService.getTodo(userId, todoId));
	}

	@PatchMapping("/{todoId}")
	public TodoResponse updateTodo(
			@AuthenticationPrincipal UUID userId,
			@PathVariable("todoId") UUID todoId,
			@RequestBody TodoUpdateRequest request) {
		return TodoResponse.from(todoService.updateTodo(userId, todoId, request));
	}

	@DeleteMapping("/{todoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTodo(
			@AuthenticationPrincipal UUID userId, @PathVariable("todoId") UUID todoId) {
		todoService.deleteTodo(userId, todoId);
	}
}
