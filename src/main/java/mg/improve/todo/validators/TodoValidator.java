package mg.improve.todo.validators;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import mg.improve.todo.domain.dto.request.TodoCreateRequest;
import mg.improve.todo.domain.dto.request.TodoUpdateRequest;
import mg.improve.todo.exception.ValidationException;

@Component
public class TodoValidator {

	private static final int MAX_TITLE_LENGTH = 255;

	public void validateCreate(TodoCreateRequest request) {
		List<String> details = new ArrayList<>();
		validateTitle(request.title(), details);
		if (request.dueDate() == null) {
			details.add("dueDate is required");
		}
		throwIfInvalid(details);
	}

	public void validateUpdate(TodoUpdateRequest request) {
		List<String> details = new ArrayList<>();
		if (request.isTitlePresent()) {
			validateTitle(request.getTitle(), details);
		}
		if (request.isDonePresent() && request.getDone() == null) {
			details.add("done must not be null");
		}
		if (request.isDueDatePresent() && request.getDueDate() == null) {
			details.add("dueDate must not be null");
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
