package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
public class GrantApplication {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	private String organizationNumber;
	private String organizationName;
	private String applicantName;
	private String applicantEmail;
	private long amount;
	private String purpose;

	@Enumerated(EnumType.STRING)
	private GrantApplicationStatus status;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "grantApplication", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("changedAt")
	private List<StatusChange> statusHistory = new ArrayList<>();

	// Notes are only ever added, so they cannot conflict with other changes and do not bump the version
	@OneToMany(mappedBy = "grantApplication", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt")
	@OptimisticLock(excluded = true)
	private List<CaseNote> notes = new ArrayList<>();

	protected GrantApplication() {
		// for JPA
	}

	public GrantApplication(String organizationNumber, String organizationName, String applicantName,
			String applicantEmail, long amount, String purpose) {
		this.organizationNumber = organizationNumber;
		this.organizationName = organizationName;
		this.applicantName = applicantName;
		this.applicantEmail = applicantEmail;
		this.amount = amount;
		this.purpose = purpose;
		this.status = GrantApplicationStatus.RECEIVED;
	}

	public void changeStatus(GrantApplicationStatus newStatus, String reason) {
		if (!status.canTransitionTo(newStatus)) {
			throw new InvalidStatusTransitionException(status, newStatus);
		}
		statusHistory.add(new StatusChange(this, status, newStatus, reason));
		status = newStatus;
	}

	public CaseNote addNote(String text) {
		var note = new CaseNote(this, text);
		notes.add(note);
		return note;
	}

	public UUID getId() {
		return id;
	}

	public String getOrganizationNumber() {
		return organizationNumber;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public String getApplicantName() {
		return applicantName;
	}

	public String getApplicantEmail() {
		return applicantEmail;
	}

	public long getAmount() {
		return amount;
	}

	public String getPurpose() {
		return purpose;
	}

	public GrantApplicationStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}

	public List<StatusChange> getStatusHistory() {
		return Collections.unmodifiableList(statusHistory);
	}

	public List<CaseNote> getNotes() {
		return Collections.unmodifiableList(notes);
	}

}
