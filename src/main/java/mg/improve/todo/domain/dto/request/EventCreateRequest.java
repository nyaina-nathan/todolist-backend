package mg.improve.todo.domain.dto.request;

import java.time.Instant;

public record EventCreateRequest(
		String title, String description, Instant startTime, Instant endTime) {
}
