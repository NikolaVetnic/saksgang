package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.time.Instant;
import java.util.UUID;

public record GrantApplicationSummary(
		UUID id,
		String organizationNumber,
		String organizationName,
		String applicantName,
		long amount,
		GrantApplicationStatus status,
		Instant createdAt) {

	static GrantApplicationSummary from(GrantApplication application) {
		return new GrantApplicationSummary(
				application.getId(),
				application.getOrganizationNumber(),
				application.getOrganizationName(),
				application.getApplicantName(),
				application.getAmount(),
				application.getStatus(),
				application.getCreatedAt());
	}

}
