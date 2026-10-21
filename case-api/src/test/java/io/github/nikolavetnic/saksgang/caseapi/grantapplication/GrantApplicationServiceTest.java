package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.nikolavetnic.saksgang.caseapi.organization.DeletedOrganizationException;
import io.github.nikolavetnic.saksgang.caseapi.organization.Organization;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistry;
import io.github.nikolavetnic.saksgang.caseapi.organization.UnknownOrganizationException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrantApplicationServiceTest {

	private static final CreateGrantApplicationRequest REQUEST = new CreateGrantApplicationRequest(
			"974760673", "Ola Nordmann", "ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget");

	private static final UUID EXISTING_ID = UUID.fromString("0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77");

	@Mock
	private GrantApplicationRepository repository;

	@Mock
	private OrganizationRegistry organizationRegistry;

	private GrantApplicationService service;

	@BeforeEach
	void createService() {
		service = new GrantApplicationService(repository, organizationRegistry);
	}

	@Test
	void createUsesOrganizationNameFromRegistryAndStartsAsReceived() {
		// given
		when(organizationRegistry.findByOrganizationNumber("974760673"))
				.thenReturn(Optional.of(new Organization("974760673", "REGISTERENHETEN I BRØNNØYSUND", null)));
		when(repository.saveAndFlush(any())).then(returnsFirstArg());

		// when
		var created = service.create(REQUEST);

		// then
		assertThat(created.organizationName()).isEqualTo("REGISTERENHETEN I BRØNNØYSUND");
		assertThat(created.applicantName()).isEqualTo("Ola Nordmann");
		assertThat(created.status()).isEqualTo(GrantApplicationStatus.RECEIVED);
	}

	@Test
	void createRejectsUnknownOrganization() {
		// given
		when(organizationRegistry.findByOrganizationNumber("974760673")).thenReturn(Optional.empty());

		// when / then
		assertThatExceptionOfType(UnknownOrganizationException.class)
				.isThrownBy(() -> service.create(REQUEST));
		verifyNoInteractions(repository);
	}

	@Test
	void createRejectsDeletedOrganization() {
		// given
		when(organizationRegistry.findByOrganizationNumber("974760673")).thenReturn(Optional.of(
				new Organization("974760673", "Nedlagt Forening", LocalDate.of(2024, 3, 9))));

		// when / then
		assertThatExceptionOfType(DeletedOrganizationException.class)
				.isThrownBy(() -> service.create(REQUEST))
				.withMessage("Organization 974760673 was deleted on 2024-03-09");
		verifyNoInteractions(repository);
	}

	@Test
	void changeStatusUpdatesStatusAndRecordsReason() {
		// given
		existingApplication();

		// when
		var updated = service.changeStatus(EXISTING_ID, 0,
				new ChangeStatusRequest(GrantApplicationStatus.IN_REVIEW, "Tildelt saksbehandler"));

		// then
		assertThat(updated.status()).isEqualTo(GrantApplicationStatus.IN_REVIEW);
		assertThat(updated.statusHistory()).singleElement().satisfies(entry -> {
			assertThat(entry.fromStatus()).isEqualTo(GrantApplicationStatus.RECEIVED);
			assertThat(entry.reason()).isEqualTo("Tildelt saksbehandler");
		});
		verify(repository).flush();
	}

	@Test
	void changeStatusLetsInvalidTransitionThrough() {
		// given
		existingApplication();

		// when / then
		assertThatExceptionOfType(InvalidStatusTransitionException.class)
				.isThrownBy(() -> service.changeStatus(EXISTING_ID, 0,
						new ChangeStatusRequest(GrantApplicationStatus.APPROVED, "Hopper over behandling")));
		verify(repository, never()).flush();
	}

	@Test
	void changeStatusRejectsOutdatedVersion() {
		// given
		var application = existingApplication();

		// when / then
		assertThatExceptionOfType(GrantApplicationModifiedException.class)
				.isThrownBy(() -> service.changeStatus(EXISTING_ID, 3,
						new ChangeStatusRequest(GrantApplicationStatus.IN_REVIEW, "Tildelt saksbehandler")))
				.withMessageContaining("expected version 3, current version 0");
		assertThat(application.getStatus()).isEqualTo(GrantApplicationStatus.RECEIVED);
		verify(repository, never()).flush();
	}

	@Test
	void addNoteAddsNoteToApplication() {
		// given
		var application = existingApplication();

		// when
		var note = service.addNote(EXISTING_ID, new AddNoteRequest("Ringte søker, budsjett kommer."));

		// then
		assertThat(note.text()).isEqualTo("Ringte søker, budsjett kommer.");
		assertThat(application.getNotes()).extracting(CaseNote::getText)
				.containsExactly("Ringte søker, budsjett kommer.");
	}

	@Test
	void addNoteThrowsNotFoundForUnknownId() {
		// given
		var id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		// when / then
		assertThatExceptionOfType(GrantApplicationNotFoundException.class)
				.isThrownBy(() -> service.addNote(id, new AddNoteRequest("Notat")));
	}

	@Test
	void getThrowsNotFoundForUnknownId() {
		// given
		var id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		// when / then
		assertThatExceptionOfType(GrantApplicationNotFoundException.class)
				.isThrownBy(() -> service.get(id));
	}

	private GrantApplication existingApplication() {
		var application = new GrantApplication("974760673", "REGISTERENHETEN I BRØNNØYSUND", "Ola Nordmann",
				"ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget");
		when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(application));
		return application;
	}

}
