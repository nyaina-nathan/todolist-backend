package mg.improve.todo.domain.dto.response;

import java.time.Instant;
import java.util.UUID;

import mg.improve.todo.domain.entity.Event;

public record EventResponse(
		UUID id,
		UUID todoId,
		String title,
		String description,
		Instant startTime,
		Instant endTime
	) {

	public static EventResponse from(Event event) {
		return new EventResponse(
				event.getId(),
				event.getTodo() == null ? null : event.getTodo().getId(),
				event.getTitle(),
				event.getDescription(),
				event.getStartTime(),
				event.getEndTime());
	}
}
