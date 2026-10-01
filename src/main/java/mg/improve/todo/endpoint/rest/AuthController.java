package mg.improve.todo.endpoint.rest;

import jakarta.servlet.http.HttpServletResponse;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.dto.RegistrationResult;
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
		RegistrationResult result = authService.register(request);
		result.cookies()
				.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()));
		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user()));
	}
}
