package mg.improve.todo.domain.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		UserResponse user
	) {
}
