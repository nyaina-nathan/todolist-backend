package mg.improve.todo.domain.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		int code,
		String message,
		Instant timestamp,
		String path,
		List<String> details
	) {

	public static ErrorResponse of(int code, String message, String path, List<String> details) {
		return new ErrorResponse(code, message, Instant.now(), path, details);
	}
}
