package mg.improve.todo.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mg.improve.todo.repository.entity.JEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository
		extends JpaRepository<JEvent, UUID>, JpaSpecificationExecutor<JEvent> {

	List<JEvent> findAllByTodoIdAndTodoUserIdOrderByStartTimeAsc(UUID todoId, UUID userId);

	Optional<JEvent> findByIdAndTodoUserId(UUID id, UUID userId);

	List<JEvent> findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
			UUID userId, Instant from, Instant to);

	Optional<JEvent> findFirstByTodoUserIdAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
			UUID userId, Instant endTime, Instant startTime);

	Optional<JEvent>
			findFirstByTodoUserIdAndIdNotAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
					UUID userId, UUID id, Instant endTime, Instant startTime);
}
