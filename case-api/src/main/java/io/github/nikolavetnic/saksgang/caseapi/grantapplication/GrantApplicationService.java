package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import io.github.nikolavetnic.saksgang.caseapi.organization.DeletedOrganizationException;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistry;
import io.github.nikolavetnic.saksgang.caseapi.organization.UnknownOrganizationException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantApplicationService {

	private final GrantApplicationRepository repository;
	private final OrganizationRegistry organizationRegistry;

	GrantApplicationService(GrantApplicationRepository repository, OrganizationRegistry organizationRegistry) {
		this.repository = repository;
		this.organizationRegistry = organizationRegistry;
	}

	@Transactional
	public GrantApplicationDetails create(CreateGrantApplicationRequest request) {
		var organization = organizationRegistry.findByOrganizationNumber(request.organizationNumber())
				.orElseThrow(() -> new UnknownOrganizationException(request.organizationNumber()));
		if (organization.isDeleted()) {
			throw new DeletedOrganizationException(organization);
		}

		var application = new GrantApplication(
				organization.organizationNumber(),
				organization.name(),
				request.applicantName(),
				request.applicantEmail(),
				request.amount(),
				request.purpose());
		return GrantApplicationDetails.from(repository.saveAndFlush(application));
	}

	/**
	 * @param expectedVersion the version the client based its decision on, taken from the ETag
	 */
	@Transactional
	public GrantApplicationDetails changeStatus(UUID id, long expectedVersion, ChangeStatusRequest request) {
		var application = findOrThrow(id);
		if (application.getVersion() != expectedVersion) {
			throw new GrantApplicationModifiedException(id, expectedVersion, application.getVersion());
		}
		application.changeStatus(request.status(), request.reason());
		// The application is already managed, so flushing is enough to insert the new history entry.
		// Calling save() would merge it and map a copy of the entry instead of the one we created.
		repository.flush();
		return GrantApplicationDetails.from(application);
	}

	@Transactional
	public GrantApplicationDetails.NoteEntry addNote(UUID id, AddNoteRequest request) {
		var application = findOrThrow(id);
		var note = application.addNote(request.text());
		repository.flush();
		return GrantApplicationDetails.NoteEntry.from(note);
	}

	@Transactional(readOnly = true)
	public Page<GrantApplicationSummary> list(GrantApplicationStatus status, Pageable pageable) {
		var applications = status == null
				? repository.findAll(pageable)
				: repository.findByStatus(status, pageable);
		return applications.map(GrantApplicationSummary::from);
	}

	@Transactional(readOnly = true)
	public GrantApplicationDetails get(UUID id) {
		return GrantApplicationDetails.from(findOrThrow(id));
	}

	private GrantApplication findOrThrow(UUID id) {
		return repository.findById(id).orElseThrow(() -> new GrantApplicationNotFoundException(id));
	}

}
