package mg.improve.todo.endpoint.rest;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

import mg.improve.todo.domain.dto.AuthResult;
import mg.improve.todo.domain.dto.LoginRequest;
import mg.improve.todo.domain.dto.RegisterRequest;
import mg.improve.todo.domain.dto.UserResponse;
import mg.improve.todo.service.AuthService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UUID userId) {
		return ResponseEntity.ok(UserResponse.from(authService.getCurrentUser(userId)));
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteCurrentUser(
			@AuthenticationPrincipal UUID userId, HttpServletResponse response) {
		List<ResponseCookie> cookies = authService.deleteCurrentUser(userId);
		addCookies(cookies, response);
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<UserResponse> respond(
			HttpStatus status, AuthResult result, HttpServletResponse response) {
		addCookies(result.cookies(), response);
		return ResponseEntity.status(status).body(UserResponse.from(result.user()));
	}

	private void addCookies(List<ResponseCookie> cookies, HttpServletResponse response) {
		cookies.forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()));
	}
}
