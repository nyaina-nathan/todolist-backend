package mg.improve.todo.validators;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import mg.improve.todo.domain.dto.request.EventCreateRequest;
import mg.improve.todo.domain.dto.request.EventUpdateRequest;
import mg.improve.todo.exception.ValidationException;

@Component
public class EventValidator {

	private static final int MAX_TITLE_LENGTH = 255;

	public void validateCreate(EventCreateRequest request) {
		List<String> details = new ArrayList<>();
		validateTitle(request.title(), details);
		if (request.startTime() == null) {
			details.add("startTime is required");
		}
		if (request.endTime() == null) {
			details.add("endTime is required");
		}
		if (request.startTime() != null && request.endTime() != null
				&& request.endTime().isBefore(request.startTime())) {
			details.add("endTime must not be before startTime");
		}
		throwIfInvalid(details);
	}

	public void validateUpdate(EventUpdateRequest request) {
		List<String> details = new ArrayList<>();
		if (request.isTitlePresent()) {
			validateTitle(request.getTitle(), details);
		}
		if (request.isStartTimePresent() && request.getStartTime() == null) {
			details.add("startTime must not be null");
		}
		if (request.isEndTimePresent() && request.getEndTime() == null) {
			details.add("endTime must not be null");
		}
		throwIfInvalid(details);
	}

	public void validatePagination(int page, int perPage) {
		List<String> details = new ArrayList<>();
		if (page < 1) {
			details.add("page must be at least 1");
		}
		if (perPage < 1) {
			details.add("perPage must be at least 1");
		}
		throwIfInvalid(details);
	}

	public void validateWithinDueDate(Instant endTime, Instant dueDate) {
		if (endTime != null && dueDate != null && endTime.isAfter(dueDate)) {
			throw new ValidationException(
					List.of("endTime must not be after the todo's due date"));
		}
	}

	private void validateTitle(String title, List<String> details) {
		if (title == null || title.isBlank()) {
			details.add("title is required");
			return;
		}
		if (title.length() > MAX_TITLE_LENGTH) {
			details.add("title must be at most " + MAX_TITLE_LENGTH + " characters");
		}
	}

	private void throwIfInvalid(List<String> details) {
		if (!details.isEmpty()) {
			throw new ValidationException(details);
		}
	}
}
