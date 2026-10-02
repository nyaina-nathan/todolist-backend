package mg.improve.todo.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import mg.improve.todo.domain.dto.PageMeta;
import mg.improve.todo.domain.dto.TodoCreateRequest;
import mg.improve.todo.domain.dto.TodoPage;
import mg.improve.todo.domain.dto.TodoResponse;
import mg.improve.todo.domain.dto.TodoUpdateRequest;
import mg.improve.todo.domain.entity.Todo;
import mg.improve.todo.domain.entity.User;
import mg.improve.todo.domain.mappers.TodoMapper;
import mg.improve.todo.exception.TodoNotFoundException;
import mg.improve.todo.repository.TodoRepository;
import mg.improve.todo.repository.entity.JTodo;
import mg.improve.todo.validators.TodoValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

	private final TodoRepository todoRepository;

	private final TodoMapper todoMapper;

	private final TodoValidator todoValidator;

	@Transactional(readOnly = true)
	public TodoPage listTodos(
			UUID userId, Boolean isDone, String title, int page, int perPage) {
		todoValidator.validatePagination(page, perPage);

		String titleFilter = (title == null || title.isBlank()) ? null : title.trim();
		Pageable pageable = PageRequest.of(
				page - 1, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<JTodo> result = todoRepository.search(userId, isDone, titleFilter, pageable);
		List<TodoResponse> items = result.getContent().stream()
				.map(todoMapper::toDomain)
				.map(TodoResponse::from)
				.toList();

		var meta = new PageMeta(
				page, perPage, result.getTotalElements(), result.getTotalPages());
		return new TodoPage(meta, items);
	}

	@Transactional
	public Todo createTodo(UUID userId, TodoCreateRequest request) {
		todoValidator.validateCreate(request);

		var user = new User();
		user.setId(userId);

		var todo = new Todo();
		todo.setUser(user);
		todo.setTitle(request.title().trim());
		todo.setDescription(request.description());
		todo.setDone(false);
		todo.setDueDate(request.dueDate());

		return todoMapper.toDomain(todoRepository.save(todoMapper.toJpa(todo)));
	}

	@Transactional(readOnly = true)
	public Todo getTodo(UUID userId, UUID todoId) {
		return todoMapper.toDomain(findOwned(userId, todoId));
	}

	@Transactional
	public Todo updateTodo(UUID userId, UUID todoId, TodoUpdateRequest request) {
		todoValidator.validateUpdate(request);

		JTodo jpa = findOwned(userId, todoId);
		if (request.isTitlePresent()) {
			jpa.setTitle(request.getTitle().trim());
		}
		if (request.isDescriptionPresent()) {
			jpa.setDescription(request.getDescription());
		}
		if (request.isDonePresent()) {
			jpa.setDone(request.getDone());
		}
		if (request.isDueDatePresent()) {
			jpa.setDueDate(request.getDueDate());
		}

		return todoMapper.toDomain(todoRepository.save(jpa));
	}

	@Transactional
	public void deleteTodo(UUID userId, UUID todoId) {
		todoRepository.delete(findOwned(userId, todoId));
	}

	private JTodo findOwned(UUID userId, UUID todoId) {
		return todoRepository.findByIdAndUserId(todoId, userId)
				.orElseThrow(TodoNotFoundException::new);
	}
}
