package mg.improve.todo.repository;

import java.util.Optional;
import java.util.UUID;
import mg.improve.todo.repository.entity.JRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<JRefreshToken, UUID> {

	Optional<JRefreshToken> findByValue(String value);

	void deleteByUser_Id(UUID userId);
}
