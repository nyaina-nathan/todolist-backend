package mg.improve.todo.exception;

import mg.improve.todo.domain.dto.response.EventResponse;

public class EventConflictException extends RuntimeException {

	private final EventResponse event;

	public EventConflictException(EventResponse event) {
		super("Event overlaps an existing event");
		this.event = event;
	}

	public EventResponse getEvent() {
		return event;
	}
}
