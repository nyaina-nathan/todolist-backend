package mg.improve.todo.repository;

import java.util.UUID;
import mg.improve.todo.repository.entity.JRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<JRefreshToken, UUID> {
}
