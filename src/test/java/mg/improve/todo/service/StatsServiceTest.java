package mg.improve.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.domain.mappers.TodoMapper;
import mg.improve.todo.repository.EventRepository;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JEvent;
import mg.improve.todo.repository.entity.JTodo;

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

	@InjectMocks
	private StatsService statsService;

	@Test
	void getStatsAggregatesUndoneTodosAndUpcomingEvents() {
		JEvent first = jEvent(UUID.randomUUID(), START, END);
		JEvent second = jEvent(
				UUID.randomUUID(), END, END.plus(Duration.ofMinutes(30)));
		JTodo closest = new JTodo();
		closest.setId(UUID.randomUUID());
		Todo closestDomain = new Todo();
		closestDomain.setId(closest.getId());
		closestDomain.setDueDate(START);

		given(todoRepository.countByUserIdAndDoneFalse(USER_ID)).willReturn(3L);
		given(eventRepository
				.findAllByTodoUserIdAndStartTimeBetweenOrderByStartTimeAsc(
						eq(USER_ID), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(first, second));
		given(todoRepository
				.findFirstByUserIdAndDoneFalseAndDueDateGreaterThanEqualOrderByDueDateAsc(
						eq(USER_ID), any(Instant.class)))
				.willReturn(Optional.of(closest));
		given(todoMapper.toDomain(closest)).willReturn(closestDomain);

		StatsResponse result = statsService.getStats(USER_ID);

		assertThat(result.undoneTodoCount()).isEqualTo(3L);
		assertThat(result.upcomingEventCount()).isEqualTo(2L);
		assertThat(result.upcomingEventsTotalLength()).isEqualTo("PT1H30M");
		assertThat(result.closestDeadlineTodo().id()).isEqualTo(closest.getId());
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

		StatsResponse result = statsService.getStats(USER_ID);

		assertThat(result.undoneTodoCount()).isZero();
		assertThat(result.upcomingEventCount()).isZero();
		assertThat(result.upcomingEventsTotalLength()).isEqualTo("PT0S");
		assertThat(result.closestDeadlineTodo()).isNull();
	}

	private JEvent jEvent(UUID id, Instant start, Instant end) {
		var jpa = new JEvent();
		jpa.setId(id);
		jpa.setStartTime(start);
		jpa.setEndTime(end);
		return jpa;
	}
}
