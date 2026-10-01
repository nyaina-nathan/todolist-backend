package mg.improve.todo.domain.mappers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.repository.entity.JUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

	@PersistenceContext
	private EntityManager entityManager;

	public User toDomain(JUser jpa) {
		if (jpa == null) {
			return null;
		}
		var domain = new User();
		domain.setId(jpa.getId());
		domain.setEmail(jpa.getEmail());
		domain.setUsername(jpa.getUsername());
		domain.setPasswordHash(jpa.getPasswordHash());
		domain.setCreatedAt(jpa.getCreatedAt());
		domain.setUpdatedAt(jpa.getUpdatedAt());
		return domain;
	}

	public JUser toJpa(User domain) {
		if (domain == null) {
			return null;
		}
		var jpa = new JUser();
		jpa.setId(domain.getId());
		jpa.setEmail(domain.getEmail());
		jpa.setUsername(domain.getUsername());
		jpa.setPasswordHash(domain.getPasswordHash());
		jpa.setCreatedAt(domain.getCreatedAt());
		jpa.setUpdatedAt(domain.getUpdatedAt());
		return jpa;
	}

	public User toReference(JUser jpa) {
		if (jpa == null) {
			return null;
		}
		var user = new User();
		user.setId(jpa.getId());
		return user;
	}

	public JUser toReference(User domain) {
		if (domain == null || domain.getId() == null) {
			return null;
		}
		return entityManager.getReference(JUser.class, domain.getId());
	}
}
