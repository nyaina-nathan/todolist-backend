package mg.improve.todo.endpoint.rest;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import mg.improve.todo.integration.AbstractControllerIT;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class StatsControllerIT extends AbstractControllerIT {

	@Test
	void statsAggregateUndoneTodosUpcomingEventsAndClosestDeadline() throws Exception {
		AuthSession session = register(uniqueEmail());
		Instant now = Instant.now();

		String closestId = createTodo(session, "Closest", now.plus(1, DAYS));
		String laterId = createTodo(session, "Later", now.plus(2, DAYS));
		String farId = createTodo(session, "Far", now.plus(30, DAYS));
		String doneId = createTodo(session, "Done", now.plus(3, DAYS));
		createTodo(session, "Overdue", now.minus(1, DAYS));
		markDone(session, doneId);

		createEvent(session, closestId, now.plus(1, HOURS), now.plus(2, HOURS));
		createEvent(session, laterId, now.plus(25, HOURS), now.plus(26, HOURS));
		createEvent(
				session, farId, now.plus(8, DAYS), now.plus(8, DAYS).plus(1, HOURS));

		mockMvc.perform(get("/stats").cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.undoneTodoCount").value(4))
				.andExpect(jsonPath("$.upcomingEventCount").value(2))
				.andExpect(jsonPath("$.upcomingEventsTotalLength").value("PT2H"))
				.andExpect(jsonPath("$.closestDeadlineTodo.id").value(closestId))
				.andExpect(jsonPath("$.closestDeadlineTodo.title").value("Closest"))
				.andExpect(jsonPath("$.events.length()").value(2))
				.andExpect(jsonPath("$.deadlines.length()").value(2))
				.andExpect(jsonPath("$.deadlines[0].id").value(closestId))
				.andExpect(jsonPath("$.deadlines[1].id").value(laterId));
	}

	@Test
	void statsForFreshUserAreZeroWithNullDeadline() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(get("/stats").cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.undoneTodoCount").value(0))
				.andExpect(jsonPath("$.upcomingEventCount").value(0))
				.andExpect(jsonPath("$.upcomingEventsTotalLength").value("PT0S"))
				.andExpect(jsonPath("$.closestDeadlineTodo").value(nullValue()))
				.andExpect(jsonPath("$.events").isEmpty())
				.andExpect(jsonPath("$.deadlines").isEmpty());
	}

	@Test
	void statsAreIsolatedPerUser() throws Exception {
		AuthSession owner = register(uniqueEmail());
		Instant now = Instant.now();
		String todoId = createTodo(owner, "Private plan", now.plus(1, DAYS));
		createEvent(owner, todoId, now.plus(1, HOURS), now.plus(2, HOURS));

		AuthSession other = register(uniqueEmail());

		mockMvc.perform(get("/stats").cookie(other.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.undoneTodoCount").value(0))
				.andExpect(jsonPath("$.upcomingEventCount").value(0))
				.andExpect(jsonPath("$.closestDeadlineTodo").value(nullValue()));
	}

	@Test
	void statsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/stats")).andExpect(status().isUnauthorized());
	}

	@Test
	void statsScopesOnlyCalendarListsToProvidedRange() throws Exception {
		AuthSession session = register(uniqueEmail());
		Instant now = Instant.now();

		String pastId = createTodo(session, "Past", now.minus(3, DAYS));
		String futureId = createTodo(session, "Future", now.plus(10, DAYS));
		String pastEventId = createEvent(
				session, pastId, now.minus(5, DAYS), now.minus(5, DAYS).plus(1, HOURS));
		String futureEventId = createEvent(
				session, futureId, now.plus(1, HOURS), now.plus(2, HOURS));

		mockMvc.perform(get("/stats")
						.cookie(session.accessCookie())
						.param("from", DateTimeFormatter.ISO_INSTANT.format(now.minus(7, DAYS)))
						.param("to", DateTimeFormatter.ISO_INSTANT.format(now.minus(1, DAYS))))
				.andExpect(status().isOk())
				// the four summaries stay on the now-based window
				.andExpect(jsonPath("$.undoneTodoCount").value(2))
				.andExpect(jsonPath("$.upcomingEventCount").value(1))
				.andExpect(jsonPath("$.upcomingEventsTotalLength").value("PT1H"))
				.andExpect(jsonPath("$.closestDeadlineTodo.id").value(futureId))
				// the calendar lists use the provided range
				.andExpect(jsonPath("$.events.length()").value(1))
				.andExpect(jsonPath("$.events[0].id").value(pastEventId))
				.andExpect(jsonPath("$.deadlines.length()").value(1))
				.andExpect(jsonPath("$.deadlines[0].id").value(pastId));

		mockMvc.perform(get("/stats").cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.upcomingEventCount").value(1))
				.andExpect(jsonPath("$.upcomingEventsTotalLength").value("PT1H"))
				.andExpect(jsonPath("$.closestDeadlineTodo.id").value(futureId))
				// without a range the calendar defaults to the next 7 days
				.andExpect(jsonPath("$.events.length()").value(1))
				.andExpect(jsonPath("$.events[0].id").value(futureEventId))
				.andExpect(jsonPath("$.deadlines").isEmpty());
	}

	@Test
	void statsRejectPartialRange() throws Exception {
		AuthSession session = register(uniqueEmail());
		Instant now = Instant.now();

		mockMvc.perform(get("/stats")
						.cookie(session.accessCookie())
						.param("from", DateTimeFormatter.ISO_INSTANT.format(now)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0]")
						.value("from and to must be provided together"));

		mockMvc.perform(get("/stats")
						.cookie(session.accessCookie())
						.param("to", DateTimeFormatter.ISO_INSTANT.format(now)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0]")
						.value("from and to must be provided together"));
	}

	@Test
	void statsRejectFromAfterTo() throws Exception {
		AuthSession session = register(uniqueEmail());
		Instant now = Instant.now();

		mockMvc.perform(get("/stats")
						.cookie(session.accessCookie())
						.param("from", DateTimeFormatter.ISO_INSTANT.format(now))
						.param("to", DateTimeFormatter.ISO_INSTANT.format(now.minus(1, DAYS))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0]").value("from must not be after to"));
	}

	private String createTodo(AuthSession session, String title, Instant dueDate)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/todos")
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"%s\",\"description\":\"desc\",\"dueDate\":\"%s\"}"
								.formatted(title, DateTimeFormatter.ISO_INSTANT.format(dueDate))))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createEvent(AuthSession session, String todoId, Instant start, Instant end)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/todos/{todoId}/events", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Session\",\"description\":\"desc\","
								+ "\"startTime\":\"%s\",\"endTime\":\"%s\"}"
								.formatted(
										DateTimeFormatter.ISO_INSTANT.format(start),
										DateTimeFormatter.ISO_INSTANT.format(end))))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void markDone(AuthSession session, String todoId) throws Exception {
		mockMvc.perform(patch("/todos/{todoId}", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"done\":true}"))
				.andExpect(status().isOk());
	}
}
