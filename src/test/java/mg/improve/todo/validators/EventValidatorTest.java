package mg.improve.todo.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import mg.improve.todo.exception.ValidationException;

class EventValidatorTest {

	private static final Instant DUE = Instant.parse("2026-02-01T10:00:00Z");

	private final EventValidator validator = new EventValidator();

	@Test
	void validateWithinDueDateAllowsEndBeforeDueDate() {
		assertThatCode(() -> validator.validateWithinDueDate(DUE.minusSeconds(60), DUE))
				.doesNotThrowAnyException();
	}

	@Test
	void validateWithinDueDateAllowsEndEqualToDueDate() {
		assertThatCode(() -> validator.validateWithinDueDate(DUE, DUE))
				.doesNotThrowAnyException();
	}

	@Test
	void validateWithinDueDateRejectsEndAfterDueDate() {
		assertThatThrownBy(() -> validator.validateWithinDueDate(DUE.plusSeconds(1), DUE))
				.isInstanceOf(ValidationException.class)
				.satisfies(ex -> assertThat(((ValidationException) ex).getDetails())
						.contains("endTime must not be after the todo's due date"));
	}

	@Test
	void validateWithinDueDateIsNullSafe() {
		assertThatCode(() -> validator.validateWithinDueDate(null, DUE))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateWithinDueDate(DUE, null))
				.doesNotThrowAnyException();
	}
}
