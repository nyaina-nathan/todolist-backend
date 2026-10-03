package mg.improve.todo.domain.dto.response;

import java.util.List;

public record StatsResponse(
		long undoneTodoCount,
		long upcomingEventCount,
		String upcomingEventsTotalLength,
		TodoResponse closestDeadlineTodo,
		List<EventResponse> events,
		List<TodoResponse> deadlines
	) {
}
