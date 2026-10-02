package mg.improve.todo.domain.dto;

import java.time.Instant;

public record EventCreateRequest(
		String title, String description, Instant startTime, Instant endTime) {
}
