package mg.improve.todo.endpoint.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import mg.improve.todo.domain.dto.EventCreateRequest;
import mg.improve.todo.domain.dto.EventPage;
import mg.improve.todo.domain.dto.EventResponse;
import mg.improve.todo.domain.dto.EventUpdateRequest;
import mg.improve.todo.service.EventService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping("/todos/{todoId}/events")
	public List<EventResponse> listTodoEvents(
			@AuthenticationPrincipal UUID userId, @PathVariable("todoId") UUID todoId) {
		return eventService.listTodoEvents(userId, todoId);
	}

	@PostMapping("/todos/{todoId}/events")
	@ResponseStatus(HttpStatus.CREATED)
	public EventResponse createTodoEvent(
			@AuthenticationPrincipal UUID userId,
			@PathVariable("todoId") UUID todoId,
			@RequestBody EventCreateRequest request) {
		return EventResponse.from(eventService.createTodoEvent(userId, todoId, request));
	}

	@GetMapping("/events")
	public EventPage listEvents(
			@AuthenticationPrincipal UUID userId,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "perPage", defaultValue = "20") int perPage) {
		return eventService.listEvents(userId, from, to, page, perPage);
	}

	@GetMapping("/events/{eventId}")
	public EventResponse getEvent(
			@AuthenticationPrincipal UUID userId, @PathVariable("eventId") UUID eventId) {
		return EventResponse.from(eventService.getEvent(userId, eventId));
	}

	@PatchMapping("/events/{eventId}")
	public EventResponse updateEvent(
			@AuthenticationPrincipal UUID userId,
			@PathVariable("eventId") UUID eventId,
			@RequestBody EventUpdateRequest request) {
		return EventResponse.from(eventService.updateEvent(userId, eventId, request));
	}

	@DeleteMapping("/events/{eventId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEvent(
			@AuthenticationPrincipal UUID userId, @PathVariable("eventId") UUID eventId) {
		eventService.deleteEvent(userId, eventId);
	}
}
