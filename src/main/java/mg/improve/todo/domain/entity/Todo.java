package mg.improve.todo.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Todo {

	private UUID id;

	private User user;

	private String title;

	private String description;

	private Boolean done;

	private OffsetDateTime createdAt;

	private OffsetDateTime dueDate;
}
