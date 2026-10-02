package mg.improve.todo.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid email or password");
	}
}
