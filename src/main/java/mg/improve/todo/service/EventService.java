package mg.improve.todo.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.dto.request.EventCreateRequest;
import mg.improve.todo.domain.dto.response.EventPage;
import mg.improve.todo.domain.dto.response.EventResponse;
import mg.improve.todo.domain.dto.request.EventUpdateRequest;
import mg.improve.todo.domain.dto.response.PageMeta;
import mg.improve.todo.domain.entity.Event;
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.domain.mappers.EventMapper;
import mg.improve.todo.exception.EventNotFoundException;
import mg.improve.todo.exception.TodoNotFoundException;
import mg.improve.todo.exception.ValidationException;
import mg.improve.todo.repository.EventRepository;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JEvent;
import mg.improve.todo.repository.entity.JTodo;
import mg.improve.todo.validators.EventValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

	private final EventRepository eventRepository;

	private final TodoRepository todoRepository;

	private final EventMapper eventMapper;

	private final EventValidator eventValidator;

	@Transactional(readOnly = true)
	public List<EventResponse> listTodoEvents(UUID userId, UUID todoId) {
		requireOwnedTodo(userId, todoId);
		return eventRepository.findAllByTodoIdAndTodoUserIdOrderByStartTimeAsc(todoId, userId)
				.stream()
				.map(eventMapper::toDomain)
				.map(EventResponse::from)
				.toList();
	}

	@Transactional
	public Event createTodoEvent(UUID userId, UUID todoId, EventCreateRequest request) {
		eventValidator.validateCreate(request);

		JTodo todo = requireOwnedTodo(userId, todoId);

		var todoRef = new Todo();
		todoRef.setId(todo.getId());

		var event = new Event();
		event.setTodo(todoRef);
		event.setTitle(request.title().trim());
		event.setDescription(request.description());
		event.setStartTime(request.startTime());
		event.setEndTime(request.endTime());

		return eventMapper.toDomain(eventRepository.save(eventMapper.toJpa(event)));
	}

	@Transactional(readOnly = true)
	public EventPage listEvents(
			UUID userId, Instant from, Instant to, int page, int perPage) {
		eventValidator.validatePagination(page, perPage);

		Pageable pageable = PageRequest.of(
				page - 1, perPage, Sort.by(Sort.Direction.ASC, "startTime"));

		Specification<JEvent> spec = (root, query, cb) -> cb.and(
				cb.equal(root.get("todo").get("user").get("id"), userId),
				from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("startTime"), from),
				to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("startTime"), to));

		Page<JEvent> result = eventRepository.findAll(spec, pageable);
		List<EventResponse> items = result.getContent().stream()
				.map(eventMapper::toDomain)
				.map(EventResponse::from)
				.toList();

		var meta = new PageMeta(
				page, perPage, result.getTotalElements(), result.getTotalPages());
		return new EventPage(meta, items);
	}

	@Transactional(readOnly = true)
	public Event getEvent(UUID userId, UUID eventId) {
		return eventMapper.toDomain(findOwned(userId, eventId));
	}

	@Transactional
	public Event updateEvent(UUID userId, UUID eventId, EventUpdateRequest request) {
		eventValidator.validateUpdate(request);

		JEvent jpa = findOwned(userId, eventId);

		Instant startTime = request.isStartTimePresent() ? request.getStartTime() : jpa.getStartTime();
		Instant endTime = request.isEndTimePresent() ? request.getEndTime() : jpa.getEndTime();
		if (endTime.isBefore(startTime)) {
			throw new ValidationException(List.of("endTime must not be before startTime"));
		}

		if (request.isTitlePresent()) {
			jpa.setTitle(request.getTitle().trim());
		}
		if (request.isDescriptionPresent()) {
			jpa.setDescription(request.getDescription());
		}
		if (request.isStartTimePresent()) {
			jpa.setStartTime(request.getStartTime());
		}
		if (request.isEndTimePresent()) {
			jpa.setEndTime(request.getEndTime());
		}

		return eventMapper.toDomain(eventRepository.save(jpa));
	}

	@Transactional
	public void deleteEvent(UUID userId, UUID eventId) {
		eventRepository.delete(findOwned(userId, eventId));
	}

	private JEvent findOwned(UUID userId, UUID eventId) {
		return eventRepository.findByIdAndTodoUserId(eventId, userId)
				.orElseThrow(EventNotFoundException::new);
	}

	private JTodo requireOwnedTodo(UUID userId, UUID todoId) {
		return todoRepository.findByIdAndUserId(todoId, userId)
				.orElseThrow(TodoNotFoundException::new);
	}
}
