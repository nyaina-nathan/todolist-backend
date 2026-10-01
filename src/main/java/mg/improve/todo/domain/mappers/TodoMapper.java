package mg.improve.todo.domain.mappers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.repository.entity.JTodo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TodoMapper {

	private final UserMapper userMapper;

	@PersistenceContext
	private EntityManager entityManager;

	public Todo toDomain(JTodo jpa) {
		if (jpa == null) {
			return null;
		}
		var domain = new Todo();
		domain.setId(jpa.getId());
		domain.setUser(userMapper.toReference(jpa.getUser()));
		domain.setTitle(jpa.getTitle());
		domain.setDescription(jpa.getDescription());
		domain.setDone(jpa.getDone());
		domain.setCreatedAt(jpa.getCreatedAt());
		domain.setDueDate(jpa.getDueDate());
		return domain;
	}

	public JTodo toJpa(Todo domain) {
		if (domain == null) {
			return null;
		}
		var jpa = new JTodo();
		jpa.setId(domain.getId());
		jpa.setUser(userMapper.toReference(domain.getUser()));
		jpa.setTitle(domain.getTitle());
		jpa.setDescription(domain.getDescription());
		jpa.setDone(domain.getDone());
		jpa.setCreatedAt(domain.getCreatedAt());
		jpa.setDueDate(domain.getDueDate());
		return jpa;
	}

	public Todo toReference(JTodo jpa) {
		if (jpa == null) {
			return null;
		}
		var todo = new Todo();
		todo.setId(jpa.getId());
		return todo;
	}

	public JTodo toReference(Todo domain) {
		if (domain == null || domain.getId() == null) {
			return null;
		}
		return entityManager.getReference(JTodo.class, domain.getId());
	}
}
