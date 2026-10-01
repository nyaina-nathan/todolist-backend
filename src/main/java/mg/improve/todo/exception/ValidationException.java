package mg.improve.todo.exception;

import java.util.List;

public class ValidationException extends RuntimeException {

	private final List<String> details;

	public ValidationException(List<String> details) {
		super("Invalid request payload");
		this.details = List.copyOf(details);
	}

	public List<String> getDetails() {
		return details;
	}
}
