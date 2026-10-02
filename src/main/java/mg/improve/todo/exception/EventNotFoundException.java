package mg.improve.todo.exception;

public class EventNotFoundException extends RuntimeException {

	public EventNotFoundException() {
		super("Event not found");
	}
}
