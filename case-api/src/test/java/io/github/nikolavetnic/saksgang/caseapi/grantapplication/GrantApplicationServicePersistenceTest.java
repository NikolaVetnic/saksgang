package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.nikolavetnic.saksgang.caseapi.TestcontainersConfiguration;
import io.github.nikolavetnic.saksgang.caseapi.organization.Organization;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs the service against a real database, where JPA behaviour such as merge and cascading matters.
 */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, GrantApplicationService.class })
class GrantApplicationServicePersistenceTest {

	@Autowired
	private GrantApplicationService service;

	@MockitoBean
	private OrganizationRegistry organizationRegistry;

	@BeforeEach
	void registerOrganization() {
		when(organizationRegistry.findByOrganizationNumber("974760673"))
				.thenReturn(Optional.of(new Organization("974760673", "REGISTERENHETEN I BRØNNØYSUND", null)));
	}

	@Test
	void addedNoteIsReturnedWithGeneratedIdAndTimestamp() {
		// given
		var application = service.create(newRequest());

		// when
		var note = service.addNote(application.id(), new AddNoteRequest("Ringte søker, budsjett kommer."));

		// then
		assertThat(note.id()).isNotNull();
		assertThat(note.createdAt()).isNotNull();
	}

	@Test
	void statusChangeIsReturnedWithTimestamp() {
		// given
		var application = service.create(newRequest());

		// when
		var updated = service.changeStatus(application.id(), application.version(),
				new ChangeStatusRequest(GrantApplicationStatus.IN_REVIEW, "Tildelt saksbehandler"));

		// then
		assertThat(updated.statusHistory()).singleElement()
				.satisfies(entry -> assertThat(entry.changedAt()).isNotNull());
		assertThat(updated.version()).isEqualTo(application.version() + 1);
	}

	@Test
	void addingNoteDoesNotChangeVersion() {
		// given
		var application = service.create(newRequest());

		// when
		service.addNote(application.id(), new AddNoteRequest("Ringte søker, budsjett kommer."));

		// then
		assertThat(service.get(application.id()).version()).isEqualTo(application.version());
	}

	private static CreateGrantApplicationRequest newRequest() {
		return new CreateGrantApplicationRequest("974760673", "Ola Nordmann", "ola.nordmann@example.com", 25_000,
				"Nye drakter til barnelaget");
	}

}
