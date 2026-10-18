package io.github.nikolavetnic.saksgang.sak.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.nikolavetnic.saksgang.sak.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class GrantApplicationRepositoryTest {

	@Autowired
	private GrantApplicationRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void savesNewApplicationWithGeneratedIdInitialStatusAndTimestamps() {
		// given
		var application = newApplication("123456785");

		// when
		var saved = repository.saveAndFlush(application);
		entityManager.clear();

		// then
		var found = repository.findById(saved.getId()).orElseThrow();
		assertThat(found.getOrganizationNumber()).isEqualTo("123456785");
		assertThat(found.getOrganizationName()).isEqualTo("Eksempel Idrettslag");
		assertThat(found.getAmount()).isEqualTo(25_000);
		assertThat(found.getStatus()).isEqualTo(GrantApplicationStatus.RECEIVED);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void storesStatusAsEnumName() {
		// given
		var saved = repository.saveAndFlush(newApplication("123456785"));

		// when
		var storedStatus = jdbcTemplate.queryForObject(
				"select status from grant_application where id = ?", String.class, saved.getId());

		// then
		assertThat(storedStatus).isEqualTo("RECEIVED");
	}

	@Test
	void savesNotesTogetherWithApplication() {
		// given
		var application = newApplication("123456785");
		application.addNote("Mangler budsjett, kontakter søker.");
		application.addNote("Budsjett mottatt på e-post.");

		// when
		var saved = repository.saveAndFlush(application);
		entityManager.clear();

		// then
		var found = repository.findById(saved.getId()).orElseThrow();
		assertThat(found.getNotes())
				.extracting(CaseNote::getText)
				.containsExactly("Mangler budsjett, kontakter søker.", "Budsjett mottatt på e-post.");
	}

	@Test
	void databaseRejectsOrganizationNumberThatIsNotNineDigits() {
		// given
		var application = newApplication("12345");

		// when / then
		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> repository.saveAndFlush(application));
	}

	private static GrantApplication newApplication(String organizationNumber) {
		return new GrantApplication(organizationNumber, "Eksempel Idrettslag", "Ola Nordmann",
				"ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget");
	}

}
