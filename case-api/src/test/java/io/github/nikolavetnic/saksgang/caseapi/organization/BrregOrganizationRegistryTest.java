package io.github.nikolavetnic.saksgang.caseapi.organization;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

/**
 * Contract test for the Brønnøysund Register Centre API. Response bodies follow the examples in the
 * official documentation at data.brreg.no/enhetsregisteret/api/dokumentasjon.
 */
class BrregOrganizationRegistryTest {

	private static final String ENTITY_V2 = "application/vnd.brreg.enhetsregisteret.enhet.v2+json";

	@RegisterExtension
	static WireMockExtension brreg = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private OrganizationRegistry registry;

	@BeforeEach
	void createRegistry() {
		var properties = new BrregProperties(URI.create(brreg.baseUrl()), Duration.ofSeconds(1),
				Duration.ofMillis(500));
		registry = new BrregOrganizationRegistry(RestClient.builder(), properties);
	}

	@Test
	void returnsActiveOrganization() {
		// given
		brreg.stubFor(get("/enhetsregisteret/api/enheter/974760673").willReturn(okForContentType(ENTITY_V2, """
				{
				  "respons_klasse": "Enhet",
				  "organisasjonsnummer": "974760673",
				  "navn": "REGISTERENHETEN I BRØNNØYSUND",
				  "organisasjonsform": {
				    "kode": "ORGL",
				    "beskrivelse": "Organisasjonsledd"
				  },
				  "registreringsdatoEnhetsregisteret": "1995-08-09",
				  "konkurs": false,
				  "underAvvikling": false
				}
				""")));

		// when
		var organization = registry.findByOrganizationNumber("974760673");

		// then
		assertThat(organization).hasValueSatisfying(found -> {
			assertThat(found.organizationNumber()).isEqualTo("974760673");
			assertThat(found.name()).isEqualTo("REGISTERENHETEN I BRØNNØYSUND");
			assertThat(found.isDeleted()).isFalse();
		});
	}

	@Test
	void returnsDeletedOrganizationWithDeletionDate() {
		// given
		brreg.stubFor(get("/enhetsregisteret/api/enheter/509100675").willReturn(okForContentType(ENTITY_V2, """
				{
				  "respons_klasse": "SlettetEnhet",
				  "organisasjonsnummer": "509100675",
				  "navn": "Sesam stasjon",
				  "organisasjonsform": {
				    "kode": "AS",
				    "beskrivelse": "Aksjeselskap"
				  },
				  "slettedato": "2024-03-09"
				}
				""")));

		// when
		var organization = registry.findByOrganizationNumber("509100675");

		// then
		assertThat(organization).hasValueSatisfying(found -> {
			assertThat(found.isDeleted()).isTrue();
			assertThat(found.deletedOn()).isEqualTo(LocalDate.of(2024, 3, 9));
		});
	}

	@Test
	void returnsEmptyForUnknownOrganizationNumber() {
		brreg.stubFor(get("/enhetsregisteret/api/enheter/123456785").willReturn(notFound()));

		assertThat(registry.findByOrganizationNumber("123456785")).isEmpty();
	}

	@Test
	void returnsEmptyForOrganizationRemovedForLegalReasons() {
		brreg.stubFor(get("/enhetsregisteret/api/enheter/509100675").willReturn(aResponse()
				.withStatus(410)
				.withHeader("Content-Type", "application/json")
				.withBody("""
						{ "organisasjonsnummer": "509100675", "slettedato": "2024-03-09" }
						""")));

		assertThat(registry.findByOrganizationNumber("509100675")).isEmpty();
	}

	@Test
	void throwsUnavailableWhenRegisterFails() {
		brreg.stubFor(get("/enhetsregisteret/api/enheter/974760673").willReturn(serverError()));

		assertThatExceptionOfType(OrganizationRegistryUnavailableException.class)
				.isThrownBy(() -> registry.findByOrganizationNumber("974760673"));
	}

	@Test
	void throwsUnavailableWhenRegisterIsTooSlow() {
		// given a response slower than the 500 ms read timeout
		brreg.stubFor(get("/enhetsregisteret/api/enheter/974760673")
				.willReturn(okForContentType(ENTITY_V2, "{}").withFixedDelay(1_000)));

		// when / then
		assertThatExceptionOfType(OrganizationRegistryUnavailableException.class)
				.isThrownBy(() -> registry.findByOrganizationNumber("974760673"));
	}

	@Test
	void requestsVersion2OfTheEntityFormat() {
		// given
		brreg.stubFor(get("/enhetsregisteret/api/enheter/974760673").willReturn(notFound()));

		// when
		registry.findByOrganizationNumber("974760673");

		// then
		brreg.verify(getRequestedFor(urlEqualTo("/enhetsregisteret/api/enheter/974760673"))
				.withHeader("Accept", equalTo(ENTITY_V2)));
	}

}
