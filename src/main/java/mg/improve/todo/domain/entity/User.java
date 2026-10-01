package mg.improve.todo.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class User {

	private UUID id;

	private String email;

	private String username;

	private String passwordHash;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;
}
