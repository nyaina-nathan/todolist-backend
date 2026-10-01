package mg.improve.todo.domain.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Event {

	private UUID id;

	private Todo todo;

	private String title;

	private String description;

	private Instant startTime;

	private Instant endTime;
}
