package mg.improve.todo.domain.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

	private UUID id;

	private User user;

	private String value;

	private Instant issuedAt;

	private Instant expiresAt;
}
