package mg.improve.todo.domain.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(
		int code,
		String message,
		Instant timestamp,
		String path,
		List<String> details,
		@JsonInclude(JsonInclude.Include.NON_NULL) EventResponse event
	) {

	public static ErrorResponse of(int code, String message, String path, List<String> details) {
		return of(code, message, path, details, null);
	}

	public static ErrorResponse of(
			int code, String message, String path, List<String> details, EventResponse event) {
		return new ErrorResponse(code, message, Instant.now(), path, details, event);
	}
}
