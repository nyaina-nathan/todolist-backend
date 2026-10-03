package mg.improve.todo.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mg.improve.todo.repository.entity.JTodo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<JTodo, UUID> {

	@Query("""
			select t from JTodo t
			where t.user.id = :userId
			and (:isDone is null or t.done = :isDone)
			and (:title is null or lower(t.title) like lower(concat('%', cast(:title as String), '%')))
			""")
	Page<JTodo> search(
			@Param("userId") UUID userId,
			@Param("isDone") Boolean isDone,
			@Param("title") String title,
			Pageable pageable);

	Optional<JTodo> findByIdAndUserId(UUID id, UUID userId);

	long countByUserIdAndDoneFalse(UUID userId);

	Optional<JTodo> findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(
			UUID userId, Instant dueDate);

	List<JTodo> findAllByUserIdAndDoneFalseAndDueDateBetweenOrderByDueDateAsc(
			UUID userId, Instant from, Instant to);
}
