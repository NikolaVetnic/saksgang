package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.APPROVED;
import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.IN_REVIEW;
import static io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus.RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class GrantApplicationTest {

	@Test
	void newApplicationIsReceivedWithEmptyHistory() {
		var application = newApplication();

		assertThat(application.getStatus()).isEqualTo(RECEIVED);
		assertThat(application.getStatusHistory()).isEmpty();
	}

	@Test
	void validStatusChangeUpdatesStatusAndRecordsHistory() {
		// given
		var application = newApplication();

		// when
		application.changeStatus(IN_REVIEW, "Tildelt saksbehandler");

		// then
		assertThat(application.getStatus()).isEqualTo(IN_REVIEW);
		assertThat(application.getStatusHistory()).singleElement().satisfies(change -> {
			assertThat(change.getFromStatus()).isEqualTo(RECEIVED);
			assertThat(change.getToStatus()).isEqualTo(IN_REVIEW);
			assertThat(change.getReason()).isEqualTo("Tildelt saksbehandler");
		});
	}

	@Test
	void historyKeepsEveryChangeInOrder() {
		// given
		var application = newApplication();

		// when
		application.changeStatus(IN_REVIEW, "Tildelt saksbehandler");
		application.changeStatus(APPROVED, "Oppfyller alle kriterier");

		// then
		assertThat(application.getStatusHistory())
				.extracting(StatusChange::getFromStatus, StatusChange::getToStatus)
				.containsExactly(
						tuple(RECEIVED, IN_REVIEW),
						tuple(IN_REVIEW, APPROVED));
	}

	@Test
	void invalidStatusChangeIsRejectedAndLeavesApplicationUnchanged() {
		// given
		var application = newApplication();

		// when / then
		assertThatExceptionOfType(InvalidStatusTransitionException.class)
				.isThrownBy(() -> application.changeStatus(APPROVED, "Hopper over behandling"))
				.withMessage("Cannot change status from RECEIVED to APPROVED")
				.satisfies(exception -> {
					assertThat(exception.getFrom()).isEqualTo(RECEIVED);
					assertThat(exception.getTo()).isEqualTo(APPROVED);
				});
		assertThat(application.getStatus()).isEqualTo(RECEIVED);
		assertThat(application.getStatusHistory()).isEmpty();
	}

	private static GrantApplication newApplication() {
		return new GrantApplication("123456785", "Eksempel Idrettslag", "Ola Nordmann",
				"ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget");
	}

}
