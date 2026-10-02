package mg.improve.todo.endpoint.rest;

import jakarta.servlet.http.HttpServletResponse;
import mg.improve.todo.domain.dto.AuthResult;
import mg.improve.todo.domain.dto.LoginRequest;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.dto.UserResponse;
import mg.improve.todo.service.AuthService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(
			@RequestBody RegisterRequest request, HttpServletResponse response) {
		AuthResult result = authService.register(request);
		return respond(HttpStatus.CREATED, result, response);
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(
			@RequestBody LoginRequest request, HttpServletResponse response) {
		AuthResult result = authService.login(request);
		return respond(HttpStatus.OK, result, response);
	}

	private ResponseEntity<UserResponse> respond(
			HttpStatus status, AuthResult result, HttpServletResponse response) {
		result.cookies()
				.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()));
		return ResponseEntity.status(status).body(UserResponse.from(result.user()));
	}
}
