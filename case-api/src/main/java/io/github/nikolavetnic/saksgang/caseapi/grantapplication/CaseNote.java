package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
public class CaseNote {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private GrantApplication grantApplication;

	private String text;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	protected CaseNote() {
		// for JPA
	}

	CaseNote(GrantApplication grantApplication, String text) {
		this.grantApplication = grantApplication;
		this.text = text;
	}

	public UUID getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
