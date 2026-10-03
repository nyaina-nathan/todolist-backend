package mg.improve.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import mg.improve.todo.domain.dto.request.EventCreateRequest;
import mg.improve.todo.domain.dto.response.EventPage;
import mg.improve.todo.domain.dto.response.EventResponse;
import mg.improve.todo.domain.dto.request.EventUpdateRequest;
import mg.improve.todo.domain.dto.response.PageMeta;
import mg.improve.todo.domain.entity.Event;
import mg.improve.todo.domain.mappers.EventMapper;
import mg.improve.todo.exception.EventConflictException;
import mg.improve.todo.exception.EventNotFoundException;
import mg.improve.todo.exception.TodoNotFoundException;
import mg.improve.todo.exception.ValidationException;
import mg.improve.todo.repository.EventRepository;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JEvent;
import mg.improve.todo.repository.entity.JTodo;
import mg.improve.todo.validators.EventValidator;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	private static final UUID USER_ID = UUID.randomUUID();

	private static final Instant START = Instant.parse("2026-02-01T09:00:00Z");

	private static final Instant END = Instant.parse("2026-02-01T10:00:00Z");

	@Mock
	private EventRepository eventRepository;

	@Mock
	private TodoRepository todoRepository;

	@Mock
	private EventMapper eventMapper;

	@Mock
	private EventValidator eventValidator;

	@InjectMocks
	private EventService eventService;

	@Test
	void listTodoEventsReturnsMappedEvents() {
		UUID todoId = UUID.randomUUID();
		JTodo ownedTodo = new JTodo();
		ownedTodo.setId(todoId);
		JEvent jpa = jEvent(UUID.randomUUID());
		Event domain = event(jpa.getId());

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(ownedTodo));
		given(eventRepository.findAllByTodoIdAndTodoUserIdOrderByStartTimeAsc(todoId, USER_ID))
				.willReturn(List.of(jpa));
		given(eventMapper.toDomain(jpa)).willReturn(domain);

		List<EventResponse> result = eventService.listTodoEvents(USER_ID, todoId);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(jpa.getId());
	}

	@Test
	void listTodoEventsThrowsWhenTodoNotOwned() {
		UUID todoId = UUID.randomUUID();
		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.listTodoEvents(USER_ID, todoId))
				.isInstanceOf(TodoNotFoundException.class);
		verifyNoInteractions(eventRepository);
	}

	@Test
	void createTodoEventTrimsTitleAndAttachesTodo() {
		UUID todoId = UUID.randomUUID();
		JTodo ownedTodo = new JTodo();
		ownedTodo.setId(todoId);
		EventCreateRequest request = new EventCreateRequest("  standup  ", "desc", START, END);
		JEvent jpa = new JEvent();
		Event mapped = event(UUID.randomUUID());

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(ownedTodo));
		given(eventMapper.toJpa(any(Event.class))).willReturn(jpa);
		given(eventRepository.save(jpa)).willReturn(jpa);
		given(eventMapper.toDomain(jpa)).willReturn(mapped);

		Event result = eventService.createTodoEvent(USER_ID, todoId, request);

		assertThat(result).isSameAs(mapped);
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventMapper).toJpa(captor.capture());
		Event captured = captor.getValue();
		assertThat(captured.getTitle()).isEqualTo("standup");
		assertThat(captured.getDescription()).isEqualTo("desc");
		assertThat(captured.getStartTime()).isEqualTo(START);
		assertThat(captured.getEndTime()).isEqualTo(END);
		assertThat(captured.getTodo().getId()).isEqualTo(todoId);
		verify(eventValidator).validateCreate(request);
	}

	@Test
	void createTodoEventRejectsInvalidRequest() {
		UUID todoId = UUID.randomUUID();
		EventCreateRequest request = new EventCreateRequest(" ", null, null, null);
		willThrow(new ValidationException(List.of("title is required")))
				.given(eventValidator).validateCreate(request);

		assertThatThrownBy(() -> eventService.createTodoEvent(USER_ID, todoId, request))
				.isInstanceOf(ValidationException.class);
		verifyNoInteractions(todoRepository, eventRepository);
	}

	@Test
	void createTodoEventThrowsWhenTodoNotOwned() {
		UUID todoId = UUID.randomUUID();
		EventCreateRequest request = new EventCreateRequest("standup", null, START, END);
		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.createTodoEvent(USER_ID, todoId, request))
				.isInstanceOf(TodoNotFoundException.class);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void createTodoEventRejectsOverlappingEvent() {
		UUID todoId = UUID.randomUUID();
		JTodo ownedTodo = new JTodo();
		ownedTodo.setId(todoId);
		EventCreateRequest request = new EventCreateRequest("standup", null, START, END);
		JEvent conflict = jEvent(UUID.randomUUID());
		Event conflictDomain = event(conflict.getId());

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(ownedTodo));
		given(eventRepository
				.findFirstByTodoUserIdAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
						USER_ID, END, START))
				.willReturn(Optional.of(conflict));
		given(eventMapper.toDomain(conflict)).willReturn(conflictDomain);

		assertThatThrownBy(() -> eventService.createTodoEvent(USER_ID, todoId, request))
				.isInstanceOf(EventConflictException.class)
				.satisfies(ex -> assertThat(((EventConflictException) ex).getEvent().id())
						.isEqualTo(conflict.getId()));
		verify(eventRepository, never()).save(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void listEventsReturnsPageWithMeta() {
		JEvent jpa = jEvent(UUID.randomUUID());
		Event domain = event(jpa.getId());
		Page<JEvent> repoPage = new PageImpl<>(List.of(jpa), PageRequest.of(0, 20), 1);

		given(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
				.willReturn(repoPage);
		given(eventMapper.toDomain(jpa)).willReturn(domain);

		EventPage result = eventService.listEvents(USER_ID, START, END, 1, 20);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).id()).isEqualTo(jpa.getId());
		assertThat(result.meta()).isEqualTo(new PageMeta(1, 20, 1, 1));
		verify(eventValidator).validatePagination(1, 20);
	}

	@Test
	@SuppressWarnings("unchecked")
	void listEventsAcceptsNullDateRange() {
		Page<JEvent> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
		given(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
				.willReturn(empty);

		EventPage result = eventService.listEvents(USER_ID, null, null, 1, 10);

		assertThat(result.items()).isEmpty();
		assertThat(result.meta().total()).isZero();
	}

	@Test
	@SuppressWarnings("unchecked")
	void listEventsRejectsInvalidPagination() {
		willThrow(new ValidationException(List.of("page must be at least 1")))
				.given(eventValidator).validatePagination(0, 10);

		assertThatThrownBy(() -> eventService.listEvents(USER_ID, null, null, 0, 10))
				.isInstanceOf(ValidationException.class);
		verifyNoInteractions(eventRepository);
	}

	@Test
	void getEventReturnsOwnedEvent() {
		UUID eventId = UUID.randomUUID();
		JEvent jpa = jEvent(eventId);
		Event domain = event(eventId);

		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.of(jpa));
		given(eventMapper.toDomain(jpa)).willReturn(domain);

		assertThat(eventService.getEvent(USER_ID, eventId)).isSameAs(domain);
	}

	@Test
	void getEventThrowsWhenNotFound() {
		UUID eventId = UUID.randomUUID();
		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.getEvent(USER_ID, eventId))
				.isInstanceOf(EventNotFoundException.class);
	}

	@Test
	void updateEventAppliesPartialChanges() {
		UUID eventId = UUID.randomUUID();
		JEvent jpa = jEvent(eventId);
		jpa.setTitle("old");
		jpa.setStartTime(START);
		jpa.setEndTime(END);
		Event mapped = new Event();

		EventUpdateRequest request = new EventUpdateRequest();
		request.setTitle("  new  ");
		request.setEndTime(END.plusSeconds(3600));

		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.of(jpa));
		given(eventRepository.save(jpa)).willReturn(jpa);
		given(eventMapper.toDomain(jpa)).willReturn(mapped);

		Event result = eventService.updateEvent(USER_ID, eventId, request);

		assertThat(result).isSameAs(mapped);
		assertThat(jpa.getTitle()).isEqualTo("new");
		assertThat(jpa.getStartTime()).isEqualTo(START);
		assertThat(jpa.getEndTime()).isEqualTo(END.plusSeconds(3600));
		verify(eventValidator).validateUpdate(request);
	}

	@Test
	void updateEventRejectsEndBeforeStart() {
		UUID eventId = UUID.randomUUID();
		JEvent jpa = jEvent(eventId);
		jpa.setStartTime(START);
		jpa.setEndTime(END);

		EventUpdateRequest request = new EventUpdateRequest();
		request.setEndTime(START.minusSeconds(60));

		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.of(jpa));

		assertThatThrownBy(() -> eventService.updateEvent(USER_ID, eventId, request))
				.isInstanceOf(ValidationException.class);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void updateEventRejectsOverlappingEvent() {
		UUID eventId = UUID.randomUUID();
		JEvent jpa = jEvent(eventId);
		jpa.setStartTime(START);
		jpa.setEndTime(END);
		JEvent conflict = jEvent(UUID.randomUUID());
		Event conflictDomain = event(conflict.getId());

		EventUpdateRequest request = new EventUpdateRequest();
		request.setEndTime(END.plusSeconds(3600));

		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.of(jpa));
		given(eventRepository
				.findFirstByTodoUserIdAndIdNotAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
						USER_ID, eventId, END.plusSeconds(3600), START))
				.willReturn(Optional.of(conflict));
		given(eventMapper.toDomain(conflict)).willReturn(conflictDomain);

		assertThatThrownBy(() -> eventService.updateEvent(USER_ID, eventId, request))
				.isInstanceOf(EventConflictException.class)
				.satisfies(ex -> assertThat(((EventConflictException) ex).getEvent().id())
						.isEqualTo(conflict.getId()));
		verify(eventRepository, never()).save(any());
	}

	@Test
	void updateEventThrowsWhenNotFound() {
		UUID eventId = UUID.randomUUID();
		EventUpdateRequest request = new EventUpdateRequest();
		request.setTitle("new");

		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.updateEvent(USER_ID, eventId, request))
				.isInstanceOf(EventNotFoundException.class);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void deleteEventDeletesOwnedEvent() {
		UUID eventId = UUID.randomUUID();
		JEvent jpa = jEvent(eventId);
		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.of(jpa));

		eventService.deleteEvent(USER_ID, eventId);

		verify(eventRepository).delete(jpa);
	}

	@Test
	void deleteEventThrowsWhenNotFound() {
		UUID eventId = UUID.randomUUID();
		given(eventRepository.findByIdAndTodoUserId(eventId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.deleteEvent(USER_ID, eventId))
				.isInstanceOf(EventNotFoundException.class);
		verify(eventRepository, never()).delete(any(JEvent.class));
	}

	private JEvent jEvent(UUID id) {
		var jpa = new JEvent();
		jpa.setId(id);
		return jpa;
	}

	private Event event(UUID id) {
		var domain = new Event();
		domain.setId(id);
		return domain;
	}
}
