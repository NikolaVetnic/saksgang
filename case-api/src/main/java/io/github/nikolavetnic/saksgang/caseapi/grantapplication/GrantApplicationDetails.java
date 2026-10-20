package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GrantApplicationDetails(
		UUID id,
		String organizationNumber,
		String organizationName,
		String applicantName,
		String applicantEmail,
		long amount,
		String purpose,
		GrantApplicationStatus status,
		Instant createdAt,
		Instant updatedAt,
		List<StatusChangeEntry> statusHistory,
		List<NoteEntry> notes) {

	public record StatusChangeEntry(
			GrantApplicationStatus fromStatus,
			GrantApplicationStatus toStatus,
			String reason,
			Instant changedAt) {
	}

	public record NoteEntry(UUID id, String text, Instant createdAt) {

		static NoteEntry from(CaseNote note) {
			return new NoteEntry(note.getId(), note.getText(), note.getCreatedAt());
		}

	}

	static GrantApplicationDetails from(GrantApplication application) {
		return new GrantApplicationDetails(
				application.getId(),
				application.getOrganizationNumber(),
				application.getOrganizationName(),
				application.getApplicantName(),
				application.getApplicantEmail(),
				application.getAmount(),
				application.getPurpose(),
				application.getStatus(),
				application.getCreatedAt(),
				application.getUpdatedAt(),
				application.getStatusHistory().stream()
						.map(change -> new StatusChangeEntry(change.getFromStatus(), change.getToStatus(),
								change.getReason(), change.getChangedAt()))
						.toList(),
				application.getNotes().stream()
						.map(NoteEntry::from)
						.toList());
	}

}
