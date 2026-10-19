package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateGrantApplicationRequest(
		@NotBlank @OrganizationNumber String organizationNumber,
		@NotBlank @Size(max = 255) String applicantName,
		@NotBlank @Email @Size(max = 254) String applicantEmail,
		@Positive long amount,
		@NotBlank @Size(max = 2000) String purpose) {
}
