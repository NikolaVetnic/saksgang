package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.util.EnumSet;
import java.util.Set;

public enum GrantApplicationStatus {
	RECEIVED,
	IN_REVIEW,
	APPROVED,
	REJECTED;

	public Set<GrantApplicationStatus> allowedNextStatuses() {
		return switch (this) {
			case RECEIVED -> EnumSet.of(IN_REVIEW);
			case IN_REVIEW -> EnumSet.of(APPROVED, REJECTED);
			case APPROVED, REJECTED -> EnumSet.noneOf(GrantApplicationStatus.class);
		};
	}

	public boolean canTransitionTo(GrantApplicationStatus next) {
		return allowedNextStatuses().contains(next);
	}

}
