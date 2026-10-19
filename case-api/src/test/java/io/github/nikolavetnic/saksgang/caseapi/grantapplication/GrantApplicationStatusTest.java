package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GrantApplicationStatusTest {

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"RECEIVED,  IN_REVIEW",
			"IN_REVIEW, APPROVED",
			"IN_REVIEW, REJECTED",
	})
	void allowsTransition(GrantApplicationStatus from, GrantApplicationStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"RECEIVED,  RECEIVED",
			"RECEIVED,  APPROVED",
			"RECEIVED,  REJECTED",
			"IN_REVIEW, RECEIVED",
			"IN_REVIEW, IN_REVIEW",
			"APPROVED,  RECEIVED",
			"APPROVED,  IN_REVIEW",
			"APPROVED,  APPROVED",
			"APPROVED,  REJECTED",
			"REJECTED,  RECEIVED",
			"REJECTED,  IN_REVIEW",
			"REJECTED,  APPROVED",
			"REJECTED,  REJECTED",
	})
	void rejectsTransition(GrantApplicationStatus from, GrantApplicationStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
	}

}
