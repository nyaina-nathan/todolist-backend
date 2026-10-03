package mg.improve.todo.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.dto.response.EventResponse;
import mg.improve.todo.domain.dto.response.StatsResponse;
import mg.improve.todo.domain.dto.response.TodoResponse;
import mg.improve.todo.domain.mappers.EventMapper;
import mg.improve.todo.domain.mappers.TodoMapper;
import mg.improve.todo.repository.EventRepository;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JEvent;
import mg.improve.todo.validators.StatsValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

	private static final int UPCOMING_WINDOW_DAYS = 7;

	private final TodoRepository todoRepository;

	private final EventRepository eventRepository;

	private final TodoMapper todoMapper;

	private final EventMapper eventMapper;

	private final StatsValidator statsValidator;

	@Transactional(readOnly = true)
	public StatsResponse getStats(UUID userId, Instant from, Instant to) {
		statsValidator.validateRange(from, to);

		Instant now = Instant.now();
		Instant horizon = now.plus(UPCOMING_WINDOW_DAYS, ChronoUnit.DAYS);

		long undoneTodoCount = todoRepository.countByUserIdAndDoneFalse(userId);

		List<JEvent> upcomingEvents = eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(userId, now, horizon);
		Duration totalLength = upcomingEvents.stream()
				.map(event -> Duration.between(event.getStartTime(), event.getEndTime()))
				.reduce(Duration.ZERO, Duration::plus);

		TodoResponse closestDeadlineTodo = todoRepository
				.findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(userId, now)
				.map(todoMapper::toDomain)
				.map(TodoResponse::from)
				.orElse(null);

		Instant windowFrom = from == null ? now : from;
		Instant windowTo = to == null ? horizon : to;

		List<JEvent> windowEvents = from == null
				? upcomingEvents
				: eventRepository
						.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
								userId, windowFrom, windowTo);
		List<EventResponse> events = windowEvents.stream()
				.map(eventMapper::toDomain)
				.map(EventResponse::from)
				.toList();

		List<TodoResponse> deadlines = todoRepository
				.findAllByUserIdAndDoneFalseAndDueDateBetweenOrderByDueDateAsc(
						userId, windowFrom, windowTo)
				.stream()
				.map(todoMapper::toDomain)
				.map(TodoResponse::from)
				.toList();

		return new StatsResponse(
				undoneTodoCount,
				upcomingEvents.size(),
				totalLength.toString(),
				closestDeadlineTodo,
				events,
				deadlines);
	}
}
