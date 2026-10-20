package io.github.nikolavetnic.saksgang.caseapi.statistics;

import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.APPROVED;
import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.IN_REVIEW;
import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.RECEIVED;
import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

	@Mock
	private StatisticsRepository repository;

	@Test
	void includesEveryStatusInWorkflowOrderAndAddsTotals() {
		// given
		when(repository.countByStatus()).thenReturn(List.of(
				new StatusCount(APPROVED, 1, 30_000),
				new StatusCount(RECEIVED, 2, 25_000)));

		// when
		var statistics = new StatisticsService(repository).grantApplicationStatistics();

		// then
		assertThat(statistics.byStatus()).containsExactly(
				new StatusCount(RECEIVED, 2, 25_000),
				new StatusCount(IN_REVIEW, 0, 0),
				new StatusCount(APPROVED, 1, 30_000),
				new StatusCount(REJECTED, 0, 0));
		assertThat(statistics.applicationCount()).isEqualTo(3);
		assertThat(statistics.totalAmount()).isEqualTo(55_000);
	}

	@Test
	void givesZeroesWhenThereAreNoApplications() {
		// given
		when(repository.countByStatus()).thenReturn(List.of());

		// when
		var statistics = new StatisticsService(repository).grantApplicationStatistics();

		// then
		assertThat(statistics.byStatus()).hasSize(4)
				.allSatisfy(count -> assertThat(count.applicationCount()).isZero());
		assertThat(statistics.totalAmount()).isZero();
	}

}
