package mg.improve.todo.domain.dto.response;

public record StatsResponse(
		long undoneTodoCount,
		long upcomingEventCount,
		String upcomingEventsTotalLength,
		TodoResponse closestDeadlineTodo
	) {
}
