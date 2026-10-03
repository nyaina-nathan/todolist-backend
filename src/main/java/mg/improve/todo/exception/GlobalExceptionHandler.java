package mg.improve.todo.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import mg.improve.todo.config.AuthCookieFactory;
import mg.improve.todo.domain.dto.response.ErrorResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final AuthCookieFactory authCookieFactory;

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

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(
			InvalidCredentialsException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
			InvalidRefreshTokenException ex, HttpServletRequest request, HttpServletResponse response) {
		authCookieFactory.clearAuthCookies()
				.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()));
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(
			UserNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(TodoNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleTodoNotFound(
			TodoNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(EventNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEventNotFound(
			EventNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return build(
				HttpStatus.BAD_REQUEST,
				"Invalid request payload",
				request,
				List.of("parameter '" + ex.getName() + "' has an invalid value"));
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status, String message, HttpServletRequest request, List<String> details) {
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(status.value(), message, request.getRequestURI(), details));
	}
}
