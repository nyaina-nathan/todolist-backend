package mg.improve.todo.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import mg.improve.todo.domain.dto.LoginRequest;
import mg.improve.todo.exception.ValidationException;

@Component
public class LoginValidator {

	private static final int MAX_EMAIL_LENGTH = 255;

	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public void validate(LoginRequest request) {
		List<String> details = new ArrayList<>();
		validateEmail(request.email(), details);
		validatePassword(request.password(), details);
		if (!details.isEmpty()) {
			throw new ValidationException(details);
		}
	}

	private void validateEmail(String email, List<String> details) {
		if (email == null || email.isBlank()) {
			details.add("email is required");
			return;
		}
		if (email.length() > MAX_EMAIL_LENGTH) {
			details.add("email must be at most " + MAX_EMAIL_LENGTH + " characters");
		}
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			details.add("email must be a valid email address");
		}
	}

	private void validatePassword(String password, List<String> details) {
		if (password == null || password.isBlank()) {
			details.add("password is required");
		}
	}
}
