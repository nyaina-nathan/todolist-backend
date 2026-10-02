package mg.improve.todo.repository;

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
}
