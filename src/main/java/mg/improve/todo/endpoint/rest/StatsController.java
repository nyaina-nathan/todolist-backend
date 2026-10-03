package mg.improve.todo.endpoint.rest;

import java.util.UUID;

import mg.improve.todo.domain.dto.response.StatsResponse;
import mg.improve.todo.service.StatsService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

	private final StatsService statsService;

	public StatsController(StatsService statsService) {
		this.statsService = statsService;
	}

	@GetMapping("/stats")
	public StatsResponse getStats(@AuthenticationPrincipal UUID userId) {
		return statsService.getStats(userId);
	}
}
