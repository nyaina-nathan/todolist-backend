package mg.improve.todo.repository;

import java.util.Optional;
import java.util.UUID;
import mg.improve.todo.repository.entity.JUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<JUser, UUID> {

	Optional<JUser> findByEmail(String email);

	boolean existsByEmail(String email);
}
