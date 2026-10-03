package mg.improve.todo.endpoint.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import mg.improve.todo.integration.AbstractControllerIT;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class EventControllerIT extends AbstractControllerIT {

	private static final String DUE_DATE = "2026-03-01T10:00:00Z";

	private static final String START = "2026-03-01T09:00:00Z";

	private static final String END = "2026-03-01T09:30:00Z";

	@Test
	void eventLifecycle() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");
		String eventId = createEvent(session, todoId, "Standup", START, END);

		mockMvc.perform(get("/todos/{todoId}/events", todoId).cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(eventId))
				.andExpect(jsonPath("$[0].todoId").value(todoId));

		mockMvc.perform(get("/events/{eventId}", eventId).cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Standup"));

		mockMvc.perform(patch("/events/{eventId}", eventId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Daily standup\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Daily standup"));

		mockMvc.perform(delete("/events/{eventId}", eventId).cookie(session.accessCookie()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/events/{eventId}", eventId).cookie(session.accessCookie()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(404));
	}

	@Test
	void listEventsSupportsDateRangeAndPagination() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint", "2026-04-01T00:00:00Z");
		createEvent(session, todoId, "Standup", START, END);
		createEvent(session, todoId, "Retro", "2026-03-02T09:00:00Z", "2026-03-02T10:00:00Z");

		mockMvc.perform(get("/events")
						.cookie(session.accessCookie())
						.param("from", "2026-02-01T00:00:00Z")
						.param("to", "2026-04-01T00:00:00Z")
						.param("page", "1")
						.param("perPage", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(2))
				.andExpect(jsonPath("$.items.length()").value(2));

		mockMvc.perform(get("/events")
						.cookie(session.accessCookie())
						.param("from", "2027-01-01T00:00:00Z")
						.param("to", "2027-02-01T00:00:00Z")
						.param("page", "1")
						.param("perPage", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(0))
				.andExpect(jsonPath("$.items").isEmpty());

		mockMvc.perform(get("/events")
						.cookie(session.accessCookie())
						.param("page", "1")
						.param("perPage", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.perPage").value(1))
				.andExpect(jsonPath("$.meta.total").value(2))
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void createEventRejectsEndBeforeStart() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");

		mockMvc.perform(post("/todos/{todoId}/events", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Broken\",\"startTime\":\"2026-03-01T10:00:00Z\","
								+ "\"endTime\":\"2026-03-01T09:00:00Z\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void createEventRejectsOverlapAndReturnsConflictingEvent() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");
		String eventId = createEvent(session, todoId, "Standup", START, END);

		mockMvc.perform(post("/todos/{todoId}/events", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson(
								"Overlap", "2026-03-01T09:15:00Z", "2026-03-01T09:45:00Z")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.event.id").value(eventId))
				.andExpect(jsonPath("$.event.title").value("Standup"));

		mockMvc.perform(get("/todos/{todoId}/events", todoId).cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void createEventRejectsOverlapAcrossTodos() throws Exception {
		AuthSession session = register(uniqueEmail());
		String firstTodoId = createTodo(session, "Plan sprint");
		String secondTodoId = createTodo(session, "Plan retro");
		String eventId = createEvent(session, firstTodoId, "Standup", START, END);

		mockMvc.perform(post("/todos/{todoId}/events", secondTodoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson(
								"Overlap", "2026-03-01T09:15:00Z", "2026-03-01T09:45:00Z")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.event.id").value(eventId));
	}

	@Test
	void createEventAllowsTouchingTimes() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");
		createEvent(session, todoId, "Standup", START, END);
		createEvent(session, todoId, "Retro", END, "2026-03-01T10:00:00Z");
	}

	@Test
	void createEventRejectsEndAfterTodoDueDate() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");

		mockMvc.perform(post("/todos/{todoId}/events", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson(
								"Too late", "2026-03-01T10:30:00Z", "2026-03-01T11:00:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.details[0]")
						.value("endTime must not be after the todo's due date"));
	}

	@Test
	void createEventAllowsEndAtTodoDueDate() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");

		createEvent(session, todoId, "Just in time", START, DUE_DATE);
	}

	@Test
	void updateEventRejectsEndAfterTodoDueDate() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint");
		String eventId = createEvent(session, todoId, "Standup", START, END);

		mockMvc.perform(patch("/events/{eventId}", eventId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"endTime\":\"2026-03-01T10:30:00Z\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.details[0]")
						.value("endTime must not be after the todo's due date"));
	}

	@Test
	void updateEventRejectsOverlap() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Plan sprint", "2026-04-01T00:00:00Z");
		String firstId = createEvent(session, todoId, "Standup", START, END);
		String secondId = createEvent(
				session, todoId, "Retro", "2026-03-01T11:00:00Z", "2026-03-01T12:00:00Z");

		mockMvc.perform(patch("/events/{eventId}", secondId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startTime\":\"2026-03-01T09:15:00Z\","
								+ "\"endTime\":\"2026-03-01T09:45:00Z\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.event.id").value(firstId));
	}

	@Test
	void eventsAreIsolatedPerUser() throws Exception {
		AuthSession owner = register(uniqueEmail());
		String todoId = createTodo(owner, "Private plan");
		String eventId = createEvent(owner, todoId, "Standup", START, END);

		AuthSession other = register(uniqueEmail());

		mockMvc.perform(get("/events/{eventId}", eventId).cookie(other.accessCookie()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/todos/{todoId}/events", todoId).cookie(other.accessCookie()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/events").cookie(other.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	@Test
	void eventsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/events"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/events/{eventId}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/todos/{todoId}/events", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}

	private String createTodo(AuthSession session, String title) throws Exception {
		return createTodo(session, title, DUE_DATE);
	}

	private String createTodo(AuthSession session, String title, String dueDate) throws Exception {
		MvcResult result = mockMvc.perform(post("/todos")
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"%s\",\"description\":\"desc\",\"dueDate\":\"%s\"}"
								.formatted(title, dueDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createEvent(
			AuthSession session, String todoId, String title, String start, String end)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/todos/{todoId}/events", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson(title, start, end)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static String eventJson(String title, String start, String end) {
		return "{\"title\":\"%s\",\"description\":\"desc\",\"startTime\":\"%s\",\"endTime\":\"%s\"}"
				.formatted(title, start, end);
	}
}
