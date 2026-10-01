package mg.improve.todo.domain.dto;

import java.time.Instant;
import java.util.UUID;
import mg.improve.todo.domain.entity.User;

public record UserResponse(
		UUID id,
		String email,
		String username,
		Instant createdAt,
		Instant updatedAt
	) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}
}
