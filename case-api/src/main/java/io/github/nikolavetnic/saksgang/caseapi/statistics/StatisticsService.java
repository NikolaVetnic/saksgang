package io.github.nikolavetnic.saksgang.caseapi.statistics;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StatisticsService {

	private final StatisticsRepository repository;

	StatisticsService(StatisticsRepository repository) {
		this.repository = repository;
	}

	/**
	 * Every status is included, also those without applications, so clients get a stable shape.
	 */
	@Transactional(readOnly = true)
	GrantApplicationStatistics grantApplicationStatistics() {
		var counted = repository.countByStatus().stream()
				.collect(Collectors.toMap(StatusCount::status, Function.identity()));

		var byStatus = Arrays.stream(GrantApplicationStatus.values())
				.map(status -> counted.getOrDefault(status, new StatusCount(status, 0, 0)))
				.toList();

		return new GrantApplicationStatistics(
				byStatus,
				byStatus.stream().mapToLong(StatusCount::applicationCount).sum(),
				byStatus.stream().mapToLong(StatusCount::totalAmount).sum());
	}

}
