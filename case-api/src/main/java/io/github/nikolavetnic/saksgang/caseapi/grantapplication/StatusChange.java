package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
public class StatusChange {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private GrantApplication grantApplication;

	@Enumerated(EnumType.STRING)
	private GrantApplicationStatus fromStatus;

	@Enumerated(EnumType.STRING)
	private GrantApplicationStatus toStatus;

	private String reason;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant changedAt;

	protected StatusChange() {
		// for JPA
	}

	StatusChange(GrantApplication grantApplication, GrantApplicationStatus fromStatus,
			GrantApplicationStatus toStatus, String reason) {
		this.grantApplication = grantApplication;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.reason = reason;
	}

	public UUID getId() {
		return id;
	}

	public GrantApplicationStatus getFromStatus() {
		return fromStatus;
	}

	public GrantApplicationStatus getToStatus() {
		return toStatus;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public String getReason() {
		return reason;
	}

}
