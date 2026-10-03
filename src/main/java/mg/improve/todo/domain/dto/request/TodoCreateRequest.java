package mg.improve.todo.domain.dto.request;

import java.time.Instant;

public record TodoCreateRequest(String title, String description, Instant dueDate) {
}
