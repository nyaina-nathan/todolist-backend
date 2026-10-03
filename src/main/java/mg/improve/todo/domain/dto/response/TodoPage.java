package mg.improve.todo.domain.dto.response;

import java.util.List;

public record TodoPage(PageMeta meta, List<TodoResponse> items) {
}
