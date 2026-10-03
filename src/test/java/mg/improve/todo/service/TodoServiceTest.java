package mg.improve.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import mg.improve.todo.domain.dto.response.PageMeta;
import mg.improve.todo.domain.dto.request.TodoCreateRequest;
import mg.improve.todo.domain.dto.response.TodoPage;
import mg.improve.todo.domain.dto.request.TodoUpdateRequest;
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.domain.mappers.TodoMapper;
import mg.improve.todo.exception.TodoNotFoundException;
import mg.improve.todo.exception.ValidationException;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JTodo;
import mg.improve.todo.validators.TodoValidator;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

	private static final UUID USER_ID = UUID.randomUUID();

	private static final Instant DUE = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	private TodoRepository todoRepository;

	@Mock
	private TodoMapper todoMapper;

	@Mock
	private TodoValidator todoValidator;

	@InjectMocks
	private TodoService todoService;

	@Test
	void listTodosReturnsMappedPageAndMeta() {
		UUID todoId = UUID.randomUUID();
		JTodo jpa = jTodo(todoId, "exam");
		Todo domain = todo(todoId, "exam");
		Page<JTodo> repoPage = new PageImpl<>(List.of(jpa), PageRequest.of(0, 5), 1);

		given(todoRepository.search(eq(USER_ID), eq(true), eq("exam"), any(Pageable.class)))
				.willReturn(repoPage);
		given(todoMapper.toDomain(jpa)).willReturn(domain);

		TodoPage result = todoService.listTodos(USER_ID, true, "  exam  ", 1, 5);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).id()).isEqualTo(todoId);
		assertThat(result.meta()).isEqualTo(new PageMeta(1, 5, 1, 1));
		verify(todoValidator).validatePagination(1, 5);
	}

	@Test
	void listTodosUsesNullTitleFilterWhenBlank() {
		Page<JTodo> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
		given(todoRepository.search(eq(USER_ID), eq(null), eq(null), any(Pageable.class)))
				.willReturn(empty);

		TodoPage result = todoService.listTodos(USER_ID, null, "   ", 1, 10);

		assertThat(result.items()).isEmpty();
		assertThat(result.meta().total()).isZero();
		verify(todoRepository).search(eq(USER_ID), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void listTodosRejectsInvalidPagination() {
		willThrow(new ValidationException(List.of("page must be at least 1")))
				.given(todoValidator).validatePagination(0, 10);

		assertThatThrownBy(() -> todoService.listTodos(USER_ID, null, null, 0, 10))
				.isInstanceOf(ValidationException.class);
		verifyNoInteractions(todoRepository);
	}

	@Test
	void createTodoTrimsTitleAndDefaultsDoneFalse() {
		UUID todoId = UUID.randomUUID();
		TodoCreateRequest request = new TodoCreateRequest("  buy milk  ", "desc", DUE);
		JTodo jpa = new JTodo();
		Todo mapped = todo(todoId, "buy milk");

		given(todoMapper.toJpa(any(Todo.class))).willReturn(jpa);
		given(todoRepository.save(jpa)).willReturn(jpa);
		given(todoMapper.toDomain(jpa)).willReturn(mapped);

		Todo result = todoService.createTodo(USER_ID, request);

		assertThat(result).isSameAs(mapped);
		ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
		verify(todoMapper).toJpa(captor.capture());
		Todo captured = captor.getValue();
		assertThat(captured.getTitle()).isEqualTo("buy milk");
		assertThat(captured.getDescription()).isEqualTo("desc");
		assertThat(captured.getDone()).isFalse();
		assertThat(captured.getDueDate()).isEqualTo(DUE);
		assertThat(captured.getUser()).extracting(User::getId).isEqualTo(USER_ID);
		verify(todoValidator).validateCreate(request);
	}

	@Test
	void createTodoRejectsInvalidRequest() {
		TodoCreateRequest request = new TodoCreateRequest(" ", null, null);
		willThrow(new ValidationException(List.of("title is required")))
				.given(todoValidator).validateCreate(request);

		assertThatThrownBy(() -> todoService.createTodo(USER_ID, request))
				.isInstanceOf(ValidationException.class);
		verifyNoInteractions(todoRepository);
	}

	@Test
	void getTodoReturnsOwnedTodo() {
		UUID todoId = UUID.randomUUID();
		JTodo jpa = new JTodo();
		Todo domain = todo(todoId, "read");

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(jpa));
		given(todoMapper.toDomain(jpa)).willReturn(domain);

		assertThat(todoService.getTodo(USER_ID, todoId)).isSameAs(domain);
	}

	@Test
	void getTodoThrowsWhenNotFound() {
		UUID todoId = UUID.randomUUID();
		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.getTodo(USER_ID, todoId))
				.isInstanceOf(TodoNotFoundException.class);
	}

	@Test
	void updateTodoAppliesOnlyPresentFields() {
		UUID todoId = UUID.randomUUID();
		JTodo jpa = new JTodo();
		jpa.setId(todoId);
		jpa.setTitle("old");
		jpa.setDescription("old desc");
		jpa.setDone(false);
		Todo mapped = new Todo();

		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setDone(true);

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(jpa));
		given(todoRepository.save(jpa)).willReturn(jpa);
		given(todoMapper.toDomain(jpa)).willReturn(mapped);

		Todo result = todoService.updateTodo(USER_ID, todoId, request);

		assertThat(result).isSameAs(mapped);
		assertThat(jpa.getDone()).isTrue();
		assertThat(jpa.getTitle()).isEqualTo("old");
		assertThat(jpa.getDescription()).isEqualTo("old desc");
		verify(todoValidator).validateUpdate(request);
	}

	@Test
	void updateTodoThrowsWhenNotFound() {
		UUID todoId = UUID.randomUUID();
		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setDone(true);

		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.updateTodo(USER_ID, todoId, request))
				.isInstanceOf(TodoNotFoundException.class);
		verify(todoRepository, never()).save(any());
	}

	@Test
	void deleteTodoDeletesOwnedTodo() {
		UUID todoId = UUID.randomUUID();
		JTodo jpa = new JTodo();
		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.of(jpa));

		todoService.deleteTodo(USER_ID, todoId);

		verify(todoRepository).delete(jpa);
	}

	@Test
	void deleteTodoThrowsWhenNotFound() {
		UUID todoId = UUID.randomUUID();
		given(todoRepository.findByIdAndUserId(todoId, USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.deleteTodo(USER_ID, todoId))
				.isInstanceOf(TodoNotFoundException.class);
		verify(todoRepository, never()).delete(any(JTodo.class));
	}

	private JTodo jTodo(UUID id, String title) {
		var jpa = new JTodo();
		jpa.setId(id);
		jpa.setTitle(title);
		jpa.setDone(true);
		return jpa;
	}

	private Todo todo(UUID id, String title) {
		var domain = new Todo();
		domain.setId(id);
		domain.setTitle(title);
		return domain;
	}
}
