package mg.improve.todo.domain.dto.response;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		UserResponse user
	) {
}
