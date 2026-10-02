package mg.improve.todo.domain.dto;

import java.util.List;

public record EventPage(PageMeta meta, List<EventResponse> items) {
}
