package mg.improve.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.improve.todo.domain.dto.response.StatsResponse;
import mg.improve.todo.domain.entity.Event;
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.domain.mappers.EventMapper;
import mg.improve.todo.domain.mappers.TodoMapper;
import mg.improve.todo.repository.EventRepository;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JEvent;
import mg.improve.todo.repository.entity.JTodo;
import mg.improve.todo.validators.StatsValidator;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

	private static final UUID USER_ID = UUID.randomUUID();

	private static final Instant START = Instant.parse("2026-02-01T09:00:00Z");

	private static final Instant END = Instant.parse("2026-02-01T10:00:00Z");

	@Mock
	private TodoRepository todoRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private TodoMapper todoMapper;

	@Mock
	private EventMapper eventMapper;

	@Mock
	private StatsValidator statsValidator;

	@InjectMocks
	private StatsService statsService;

	@Test
	void getStatsAggregatesSummaryAndDefaultsCalendarToNextWeek() {
		JEvent first = jEvent(UUID.randomUUID(), START, END);
		JEvent second = jEvent(
				UUID.randomUUID(), END, END.plus(Duration.ofMinutes(30)));
		JTodo closest = jTodo(UUID.randomUUID(), START);
		JTodo deadline = jTodo(UUID.randomUUID(), START.plusSeconds(7200));

		given(todoRepository.countByUserIdAndDoneFalse(USER_ID)).willReturn(3L);
		given(eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(first, second));
		given(todoRepository
				.findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class)))
				.willReturn(Optional.of(closest));
		given(todoRepository
				.findAllByUserIdAndDoneFalseAndDueDateBetweenOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(deadline));
		given(todoMapper.toDomain(closest)).willReturn(todo(closest.getId()));
		given(todoMapper.toDomain(deadline)).willReturn(todo(deadline.getId()));
		given(eventMapper.toDomain(first)).willReturn(event(first.getId()));
		given(eventMapper.toDomain(second)).willReturn(event(second.getId()));

		StatsResponse result = statsService.getStats(USER_ID, null, null);

		assertThat(result.undoneTodoCount()).isEqualTo(3L);
		assertThat(result.upcomingEventCount()).isEqualTo(2L);
		assertThat(result.upcomingEventsTotalLength()).isEqualTo("PT1H30M");
		assertThat(result.closestDeadlineTodo().id()).isEqualTo(closest.getId());
		assertThat(result.events()).hasSize(2);
		assertThat(result.events().get(0).id()).isEqualTo(first.getId());
		assertThat(result.deadlines()).hasSize(1);
		assertThat(result.deadlines().get(0).id()).isEqualTo(deadline.getId());
		verify(statsValidator).validateRange(null, null);
	}

	@Test
	void getStatsScopesOnlyCalendarListsToProvidedRange() {
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-01-08T00:00:00Z");
		JEvent futureEvent = jEvent(UUID.randomUUID(), START, END);
		JEvent pastEvent = jEvent(
				UUID.randomUUID(), from.plusSeconds(3600), from.plusSeconds(7200));
		JTodo futureDeadline = jTodo(UUID.randomUUID(), START.plusSeconds(60));
		JTodo pastDeadline = jTodo(UUID.randomUUID(), from.plusSeconds(1800));

		given(todoRepository.countByUserIdAndDoneFalse(USER_ID)).willReturn(5L);
		given(eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(futureEvent));
		given(eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
						USER_ID, from, to))
				.willReturn(List.of(pastEvent));
		given(todoRepository
				.findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class)))
				.willReturn(Optional.of(futureDeadline));
		given(todoRepository
				.findAllByUserIdAndDoneFalseAndDueDateBetweenOrderByDueDateAsc(
						USER_ID, from, to))
				.willReturn(List.of(pastDeadline));
		given(todoMapper.toDomain(futureDeadline))
				.willReturn(todo(futureDeadline.getId()));
		given(todoMapper.toDomain(pastDeadline)).willReturn(todo(pastDeadline.getId()));
		given(eventMapper.toDomain(pastEvent)).willReturn(event(pastEvent.getId()));

		StatsResponse result = statsService.getStats(USER_ID, from, to);

		// summary stays on the now-based window
		assertThat(result.undoneTodoCount()).isEqualTo(5L);
		assertThat(result.upcomingEventCount()).isEqualTo(1L);
		assertThat(result.upcomingEventsTotalLength()).isEqualTo("PT1H");
		assertThat(result.closestDeadlineTodo().id()).isEqualTo(futureDeadline.getId());

		// calendar lists use the provided range
		assertThat(result.events()).hasSize(1);
		assertThat(result.events().get(0).id()).isEqualTo(pastEvent.getId());
		assertThat(result.deadlines()).hasSize(1);
		assertThat(result.deadlines().get(0).id()).isEqualTo(pastDeadline.getId());
		verify(statsValidator).validateRange(from, to);
	}

	@Test
	void getStatsReturnsZerosAndNullDeadlineWhenNothingMatches() {
		given(todoRepository.countByUserIdAndDoneFalse(USER_ID)).willReturn(0L);
		given(eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of());
		given(todoRepository
				.findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class)))
				.willReturn(Optional.empty());
		given(todoRepository
				.findAllByUserIdAndDoneFalseAndDueDateBetweenOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of());

		StatsResponse result = statsService.getStats(USER_ID, null, null);

		assertThat(result.undoneTodoCount()).isZero();
		assertThat(result.upcomingEventCount()).isZero();
		assertThat(result.upcomingEventsTotalLength()).isEqualTo("PT0S");
		assertThat(result.closestDeadlineTodo()).isNull();
		assertThat(result.events()).isEmpty();
		assertThat(result.deadlines()).isEmpty();
	}

	private JEvent jEvent(UUID id, Instant start, Instant end) {
		var jpa = new JEvent();
		jpa.setId(id);
		jpa.setStartTime(start);
		jpa.setEndTime(end);
		return jpa;
	}

	private JTodo jTodo(UUID id, Instant dueDate) {
		var jpa = new JTodo();
		jpa.setId(id);
		jpa.setDueDate(dueDate);
		return jpa;
	}

	private Todo todo(UUID id) {
		var domain = new Todo();
		domain.setId(id);
		return domain;
	}

	private Event event(UUID id) {
		var domain = new Event();
		domain.setId(id);
		return domain;
	}
}
