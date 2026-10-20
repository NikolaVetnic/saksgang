package io.github.nikolavetnic.saksgang.caseapi.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.InvalidStatusTransitionException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * The status endpoint arrives in a later change, so the mapping is tested directly for now.
 */
class InvalidStatusTransitionProblemTest {

	@Test
	void invalidTransitionGivesConflictWithCurrentAndAllowedStatuses() {
		// given
		var exception = new InvalidStatusTransitionException(GrantApplicationStatus.RECEIVED,
				GrantApplicationStatus.APPROVED);

		// when
		var problem = new ApiExceptionHandler().handleInvalidTransition(exception);

		// then
		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getTitle()).isEqualTo("Invalid status transition");
		assertThat(problem.getDetail()).isEqualTo("Cannot change status from RECEIVED to APPROVED");
		assertThat(problem.getProperties())
				.containsEntry("currentStatus", GrantApplicationStatus.RECEIVED)
				.containsEntry("allowedStatuses", Set.of(GrantApplicationStatus.IN_REVIEW));
	}

}
