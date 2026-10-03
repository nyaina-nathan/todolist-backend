package mg.improve.todo.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import mg.improve.todo.exception.ValidationException;

class StatsValidatorTest {

	private final StatsValidator validator = new StatsValidator();

	private static final Instant NOW = Instant.parse("2026-02-01T00:00:00Z");

	@Test
	void acceptsMissingRange() {
		assertThatCode(() -> validator.validateRange(null, null))
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsCompleteOrderedRange() {
		assertThatCode(() -> validator.validateRange(NOW, NOW.plusSeconds(3600)))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsFromWithoutTo() {
		assertThatThrownBy(() -> validator.validateRange(NOW, null))
				.isInstanceOf(ValidationException.class)
				.satisfies(ex -> assertThat(((ValidationException) ex).getDetails())
						.contains("from and to must be provided together"));
	}

	@Test
	void rejectsToWithoutFrom() {
		assertThatThrownBy(() -> validator.validateRange(null, NOW))
				.isInstanceOf(ValidationException.class)
				.satisfies(ex -> assertThat(((ValidationException) ex).getDetails())
						.contains("from and to must be provided together"));
	}

	@Test
	void rejectsFromAfterTo() {
		assertThatThrownBy(
				() -> validator.validateRange(NOW.plusSeconds(60), NOW))
				.isInstanceOf(ValidationException.class)
				.satisfies(ex -> assertThat(((ValidationException) ex).getDetails())
						.contains("from must not be after to"));
	}
}
