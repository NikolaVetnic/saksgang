package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import io.github.nikolavetnic.saksgang.caseapi.TestcontainersConfiguration;
import io.github.nikolavetnic.saksgang.caseapi.organization.Organization;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Real, committed transactions against PostgreSQL. Not {@code @DataJpaTest}, which would wrap everything in a
 * single rolled-back transaction and hide the conflict.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GrantApplicationConcurrencyTest {

	@Autowired
	private GrantApplicationService service;

	@Autowired
	private GrantApplicationRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@MockitoBean
	private OrganizationRegistry organizationRegistry;

	private UUID id;

	@BeforeEach
	void createApplication() {
		when(organizationRegistry.findByOrganizationNumber("974760673"))
				.thenReturn(Optional.of(new Organization("974760673", "REGISTERENHETEN I BRØNNØYSUND", null)));
		id = service.create(new CreateGrantApplicationRequest("974760673", "Ola Nordmann",
				"ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget")).id();
		service.changeStatus(id, 0, new ChangeStatusRequest(GrantApplicationStatus.IN_REVIEW, "Tildelt saksbehandler"));
	}

	@AfterEach
	void deleteApplications() {
		repository.deleteAll();
	}

	@Test
	void secondChangeBasedOnTheSameVersionIsRejected() {
		// given one case officer approves based on version 1
		service.changeStatus(id, 1, new ChangeStatusRequest(GrantApplicationStatus.APPROVED, "Oppfyller kriteriene"));

		// when / then another case officer, who also saw version 1, tries to reject
		assertThatExceptionOfType(GrantApplicationModifiedException.class)
				.isThrownBy(() -> service.changeStatus(id, 1,
						new ChangeStatusRequest(GrantApplicationStatus.REJECTED, "Mangler budsjett")));
	}

	@Test
	void simultaneousChangesAreCaughtByTheDatabaseUpdate() {
		var outer = new TransactionTemplate(transactionManager);
		var inner = new TransactionTemplate(transactionManager);
		inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
				outer.executeWithoutResult(firstTransaction -> {
					// given both transactions have read version 1
					var application = repository.findById(id).orElseThrow();

					// when the other transaction approves and commits first
					inner.executeWithoutResult(secondTransaction -> {
						var sameApplication = repository.findById(id).orElseThrow();
						sameApplication.changeStatus(GrantApplicationStatus.APPROVED, "Oppfyller kriteriene");
					});

					// then this transaction's update no longer matches the version in the database
					application.changeStatus(GrantApplicationStatus.REJECTED, "Mangler budsjett");
					repository.flush();
				}));
	}

}
