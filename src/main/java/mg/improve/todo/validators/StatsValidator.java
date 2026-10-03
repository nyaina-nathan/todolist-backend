package mg.improve.todo.validators;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import mg.improve.todo.exception.ValidationException;

@Component
public class StatsValidator {

	public void validateRange(Instant from, Instant to) {
		List<String> details = new ArrayList<>();
		if ((from == null) != (to == null)) {
			details.add("from and to must be provided together");
		}
		if (from != null && to != null && from.isAfter(to)) {
			details.add("from must not be after to");
		}
		if (!details.isEmpty()) {
			throw new ValidationException(details);
		}
	}
}
