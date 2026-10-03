package mg.improve.todo.endpoint.rest;

import java.time.Instant;
import java.util.UUID;

import mg.improve.todo.domain.dto.response.StatsResponse;
import mg.improve.todo.service.StatsService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

	private final StatsService statsService;

	public StatsController(StatsService statsService) {
		this.statsService = statsService;
	}

	@GetMapping("/stats")
	public StatsResponse getStats(
			@AuthenticationPrincipal UUID userId,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return statsService.getStats(userId, from, to);
	}
}
