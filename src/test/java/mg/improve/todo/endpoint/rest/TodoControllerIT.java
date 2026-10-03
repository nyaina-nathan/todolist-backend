package mg.improve.todo.endpoint.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import mg.improve.todo.integration.AbstractControllerIT;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TodoControllerIT extends AbstractControllerIT {

	private static final String DUE_DATE = "2026-03-01T10:00:00Z";

	@Test
	void todoLifecycle() throws Exception {
		AuthSession session = register(uniqueEmail());
		String todoId = createTodo(session, "Buy milk");

		mockMvc.perform(get("/todos/{todoId}", todoId).cookie(session.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Buy milk"))
				.andExpect(jsonPath("$.done").value(false));

		mockMvc.perform(patch("/todos/{todoId}", todoId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Buy oat milk\",\"done\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Buy oat milk"))
				.andExpect(jsonPath("$.done").value(true));

		mockMvc.perform(delete("/todos/{todoId}", todoId).cookie(session.accessCookie()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/todos/{todoId}", todoId).cookie(session.accessCookie()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(404));
	}

	@Test
	void listTodosSupportsFiltersAndPagination() throws Exception {
		AuthSession session = register(uniqueEmail());
		String alphaId = createTodo(session, "Alpha task");
		createTodo(session, "Beta task");
		createTodo(session, "Gamma task");

		mockMvc.perform(patch("/todos/{todoId}", alphaId)
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"done\":true}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/todos")
						.cookie(session.accessCookie())
						.param("isDone", "true")
						.param("page", "1")
						.param("perPage", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Alpha task"));

		mockMvc.perform(get("/todos")
						.cookie(session.accessCookie())
						.param("title", "bet")
						.param("page", "1")
						.param("perPage", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Beta task"));

		mockMvc.perform(get("/todos")
						.cookie(session.accessCookie())
						.param("page", "1")
						.param("perPage", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.perPage").value(2))
				.andExpect(jsonPath("$.meta.total").value(3))
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	void createTodoRejectsInvalidPayload() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(post("/todos")
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"   \",\"description\":\"desc\",\"dueDate\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void listTodosRejectsInvalidPagination() throws Exception {
		AuthSession session = register(uniqueEmail());

		mockMvc.perform(get("/todos").cookie(session.accessCookie()).param("page", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void todosRequireAuthentication() throws Exception {
		mockMvc.perform(get("/todos"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/todos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"No auth\",\"dueDate\":\"%s\"}".formatted(DUE_DATE)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void todosAreIsolatedPerUser() throws Exception {
		AuthSession owner = register(uniqueEmail());
		String todoId = createTodo(owner, "Private task");

		AuthSession other = register(uniqueEmail());

		mockMvc.perform(get("/todos/{todoId}", todoId).cookie(other.accessCookie()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/todos").cookie(other.accessCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	private String createTodo(AuthSession session, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/todos")
						.cookie(session.accessCookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"%s\",\"description\":\"desc\",\"dueDate\":\"%s\"}"
								.formatted(title, DUE_DATE)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}
}
