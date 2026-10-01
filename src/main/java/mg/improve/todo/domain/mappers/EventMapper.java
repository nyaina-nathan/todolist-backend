package mg.improve.todo.domain.mappers;

import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.entity.Event;
import mg.improve.todo.repository.entity.JEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventMapper {

	private final TodoMapper todoMapper;

	public Event toDomain(JEvent jpa) {
		if (jpa == null) {
			return null;
		}
		var domain = new Event();
		domain.setId(jpa.getId());
		domain.setTodo(todoMapper.toReference(jpa.getTodo()));
		domain.setTitle(jpa.getTitle());
		domain.setDescription(jpa.getDescription());
		domain.setStartTime(jpa.getStartTime());
		domain.setEndTime(jpa.getEndTime());
		return domain;
	}

	public JEvent toJpa(Event domain) {
		if (domain == null) {
			return null;
		}
		var jpa = new JEvent();
		jpa.setId(domain.getId());
		jpa.setTodo(todoMapper.toReference(domain.getTodo()));
		jpa.setTitle(domain.getTitle());
		jpa.setDescription(domain.getDescription());
		jpa.setStartTime(domain.getStartTime());
		jpa.setEndTime(domain.getEndTime());
		return jpa;
	}
}
