package io.github.nikolavetnic.saksgang.caseapi.statistics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
class StatisticsController {

	private final StatisticsService service;

	StatisticsController(StatisticsService service) {
		this.service = service;
	}

	@GetMapping
	GrantApplicationStatistics statistics() {
		return service.grantApplicationStatistics();
	}

}
