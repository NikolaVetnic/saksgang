package io.github.nikolavetnic.saksgang.caseapi.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikolavetnic.saksgang.caseapi.TestcontainersConfiguration;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplication;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationRepository;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({ TestcontainersConfiguration.class, StatisticsRepository.class })
class StatisticsRepositoryTest {

	@Autowired
	private StatisticsRepository statisticsRepository;

	@Autowired
	private GrantApplicationRepository grantApplicationRepository;

	@Test
	void countsApplicationsAndSumsAmountsPerStatus() {
		// given
		save(10_000);
		save(15_000);
		var inReview = newApplication(40_000);
		inReview.changeStatus(GrantApplicationStatus.IN_REVIEW, "Tildelt saksbehandler");
		grantApplicationRepository.saveAndFlush(inReview);

		// when
		var counts = statisticsRepository.countByStatus();

		// then
		assertThat(counts).containsExactlyInAnyOrder(
				new StatusCount(GrantApplicationStatus.RECEIVED, 2, 25_000),
				new StatusCount(GrantApplicationStatus.IN_REVIEW, 1, 40_000));
	}

	@Test
	void returnsNothingWhenThereAreNoApplications() {
		assertThat(statisticsRepository.countByStatus()).isEmpty();
	}

	private void save(long amount) {
		grantApplicationRepository.saveAndFlush(newApplication(amount));
	}

	private static GrantApplication newApplication(long amount) {
		return new GrantApplication("123456785", "Eksempel Idrettslag", "Ola Nordmann", "ola.nordmann@example.com",
				amount, "Nye drakter til barnelaget");
	}

}
