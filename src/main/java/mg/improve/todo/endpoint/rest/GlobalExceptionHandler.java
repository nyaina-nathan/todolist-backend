package mg.improve.todo.endpoint.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import mg.improve.todo.service.EmailAlreadyUsedException;
import mg.improve.todo.validators.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			ValidationException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getDetails());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadable(
			HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(
				HttpStatus.BAD_REQUEST,
				"Invalid request payload",
				request,
				List.of("request body is malformed"));
	}

	@ExceptionHandler(EmailAlreadyUsedException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
			EmailAlreadyUsedException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status, String message, HttpServletRequest request, List<String> details) {
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(status.value(), message, request.getRequestURI(), details));
	}
}
