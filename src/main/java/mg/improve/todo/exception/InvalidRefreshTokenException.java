package mg.improve.todo.exception;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Invalid refresh token");
	}
}
