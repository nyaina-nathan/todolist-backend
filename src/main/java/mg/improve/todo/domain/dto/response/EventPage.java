package mg.improve.todo.domain.dto.response;

import java.util.List;

public record EventPage(PageMeta meta, List<EventResponse> items) {
}
