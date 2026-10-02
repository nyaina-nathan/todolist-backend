package mg.improve.todo.endpoint.rest;

import java.util.UUID;

import mg.improve.todo.domain.dto.TodoCreateRequest;
import mg.improve.todo.domain.dto.TodoPage;
import mg.improve.todo.domain.dto.TodoResponse;
import mg.improve.todo.domain.dto.TodoUpdateRequest;
import mg.improve.todo.service.TodoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
public class TodoController {

	private final TodoService todoService;

	public TodoController(TodoService todoService) {
		this.todoService = todoService;
	}

	@GetMapping
	public ResponseEntity<TodoPage> listTodos(
			@AuthenticationPrincipal UUID userId,
			@RequestParam(required = false) Boolean isDone,
			@RequestParam(required = false) String title,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int perPage) {
		return ResponseEntity.ok(todoService.listTodos(userId, isDone, title, page, perPage));
	}

	@PostMapping
	public ResponseEntity<TodoResponse> createTodo(
			@AuthenticationPrincipal UUID userId,
			@RequestBody TodoCreateRequest request) {
		TodoResponse body = TodoResponse.from(todoService.createTodo(userId, request));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{todoId}")
	public ResponseEntity<TodoResponse> getTodo(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID todoId) {
		return ResponseEntity.ok(TodoResponse.from(todoService.getTodo(userId, todoId)));
	}

	@PatchMapping("/{todoId}")
	public ResponseEntity<TodoResponse> updateTodo(
			@AuthenticationPrincipal UUID userId,
			@PathVariable UUID todoId,
			@RequestBody TodoUpdateRequest request) {
		return ResponseEntity.ok(TodoResponse.from(todoService.updateTodo(userId, todoId, request)));
	}

	@DeleteMapping("/{todoId}")
	public ResponseEntity<Void> deleteTodo(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID todoId) {
		todoService.deleteTodo(userId, todoId);
		return ResponseEntity.noContent().build();
	}
}
