package mg.improve.todo.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
public class JTodo {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_user", nullable = false)
	private JUser user;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "is_done")
	private Boolean done = false;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "due_date", nullable = false)
	private Instant dueDate;

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}
}
