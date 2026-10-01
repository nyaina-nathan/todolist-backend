package mg.improve.todo.service;

public class EmailAlreadyUsedException extends RuntimeException {

	private final String email;

	public EmailAlreadyUsedException(String email) {
		super("Email already registered: " + email);
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
