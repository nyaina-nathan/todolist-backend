package mg.improve.todo.domain.dto.response;

import java.time.Instant;
import java.util.UUID;

import mg.improve.todo.domain.entity.Todo;

public record TodoResponse(
		UUID id,
		UUID userId,
		String title,
		String description,
		boolean done,
		Instant createdAt,
		Instant dueDate
	) {

	public static TodoResponse from(Todo todo) {
		return new TodoResponse(
				todo.getId(),
				todo.getUser() == null ? null : todo.getUser().getId(),
				todo.getTitle(),
				todo.getDescription(),
				Boolean.TRUE.equals(todo.getDone()),
				todo.getCreatedAt(),
				todo.getDueDate());
	}
}
