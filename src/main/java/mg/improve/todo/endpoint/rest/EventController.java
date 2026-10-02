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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping("/todos/{todoId}/events")
	public ResponseEntity<List<EventResponse>> listTodoEvents(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID todoId) {
		return ResponseEntity.ok(eventService.listTodoEvents(userId, todoId));
	}

	@PostMapping("/todos/{todoId}/events")
	public ResponseEntity<EventResponse> createTodoEvent(
			@AuthenticationPrincipal UUID userId,
			@PathVariable UUID todoId,
			@RequestBody EventCreateRequest request) {
		EventResponse body = EventResponse.from(eventService.createTodoEvent(userId, todoId, request));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/events")
	public ResponseEntity<EventPage> listEvents(
			@AuthenticationPrincipal UUID userId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int perPage) {
		return ResponseEntity.ok(eventService.listEvents(userId, from, to, page, perPage));
	}

	@GetMapping("/events/{eventId}")
	public ResponseEntity<EventResponse> getEvent(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID eventId) {
		return ResponseEntity.ok(EventResponse.from(eventService.getEvent(userId, eventId)));
	}

	@PatchMapping("/events/{eventId}")
	public ResponseEntity<EventResponse> updateEvent(
			@AuthenticationPrincipal UUID userId,
			@PathVariable UUID eventId,
			@RequestBody EventUpdateRequest request) {
		return ResponseEntity.ok(
				EventResponse.from(eventService.updateEvent(userId, eventId, request)));
	}

	@DeleteMapping("/events/{eventId}")
	public ResponseEntity<Void> deleteEvent(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID eventId) {
		eventService.deleteEvent(userId, eventId);
		return ResponseEntity.noContent().build();
	}
}
