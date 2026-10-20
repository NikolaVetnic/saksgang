package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
		@NotNull GrantApplicationStatus status,
		@NotBlank @Size(max = 2000) String reason) {
}
