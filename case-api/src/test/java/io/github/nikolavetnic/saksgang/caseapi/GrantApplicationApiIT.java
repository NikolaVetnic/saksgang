package io.github.nikolavetnic.saksgang.caseapi;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationDetails;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/**
 * The whole API over real HTTP: a running server, PostgreSQL in a container and the Brønnøysund Register
 * Centre simulated with WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class GrantApplicationApiIT {

	private static final String ENTITY_V2 = "application/vnd.brreg.enhetsregisteret.enhet.v2+json";
	private static final String ACTIVE_ORGANIZATION = "974760673";
	private static final String UNKNOWN_ORGANIZATION = "123456785";
	private static final String DELETED_ORGANIZATION = "509100675";
	private static final String ORGANIZATION_WHEN_REGISTER_FAILS = "889640782";

	@RegisterExtension
	static WireMockExtension brreg = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void pointClientAtWireMock(DynamicPropertyRegistry registry) {
		registry.add("brreg.base-url", brreg::baseUrl);
	}

	@Autowired
	private RestTestClient client;

	@Autowired
	private GrantApplicationRepository repository;

	@BeforeEach
	void stubRegister() {
		brreg.stubFor(get("/enhetsregisteret/api/enheter/" + ACTIVE_ORGANIZATION)
				.willReturn(okForContentType(ENTITY_V2, """
						{
						  "respons_klasse": "Enhet",
						  "organisasjonsnummer": "974760673",
						  "navn": "REGISTERENHETEN I BRØNNØYSUND",
						  "organisasjonsform": { "kode": "ORGL", "beskrivelse": "Organisasjonsledd" }
						}
						""")));
		brreg.stubFor(get("/enhetsregisteret/api/enheter/" + UNKNOWN_ORGANIZATION).willReturn(notFound()));
		brreg.stubFor(get("/enhetsregisteret/api/enheter/" + DELETED_ORGANIZATION)
				.willReturn(okForContentType(ENTITY_V2, """
						{
						  "respons_klasse": "SlettetEnhet",
						  "organisasjonsnummer": "509100675",
						  "navn": "Sesam stasjon",
						  "organisasjonsform": { "kode": "AS", "beskrivelse": "Aksjeselskap" },
						  "slettedato": "2024-03-09"
						}
						""")));
		brreg.stubFor(get("/enhetsregisteret/api/enheter/" + ORGANIZATION_WHEN_REGISTER_FAILS)
				.willReturn(serverError()));
	}

	@AfterEach
	void deleteApplications() {
		repository.deleteAll();
	}

	@Test
	void caseOfficerProcessesAnApplicationFromSubmissionToApproval() {
		// An applicant submits an application; the organization name comes from the register
		var created = client.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.body(applicationFor(ACTIVE_ORGANIZATION))
				.exchange();
		assertThat(RestTestClientResponse.from(created)).hasStatus(HttpStatus.CREATED)
				.hasHeader("ETag", "\"0\"")
				.containsHeader("Location");
		var application = created.returnResult(GrantApplicationDetails.class).getResponseBody();
		assertThat(application.organizationName()).isEqualTo("REGISTERENHETEN I BRØNNØYSUND");
		var id = application.id();

		// It shows up in the case officer's list of new applications
		var list = client.get().uri("/api/grant-applications?status=RECEIVED").exchange();
		assertThat(RestTestClientResponse.from(list)).bodyJson()
				.extractingPath("$.content[*].id").asArray().containsExactly(id.toString());

		// The case officer takes it into review, based on the version they have seen
		var inReview = changeStatus(id, "\"0\"", "IN_REVIEW", "Tildelt saksbehandler");
		assertThat(RestTestClientResponse.from(inReview)).hasStatusOk().hasHeader("ETag", "\"1\"");

		// A note does not change the version
		var note = client.post().uri("/api/grant-applications/{id}/notes", id)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("text", "Ringte søker, budsjett kommer på e-post."))
				.exchange();
		assertThat(RestTestClientResponse.from(note)).hasStatus(HttpStatus.CREATED);
		assertThat(RestTestClientResponse.from(note)).bodyJson().extractingPath("$.id").isNotNull();

		// The case officer approves
		var approved = changeStatus(id, "\"1\"", "APPROVED", "Oppfyller kriteriene");
		assertThat(RestTestClientResponse.from(approved)).hasStatusOk().hasHeader("ETag", "\"2\"");

		// The case shows the full history and the note
		var details = client.get().uri("/api/grant-applications/{id}", id).exchange();
		assertThat(RestTestClientResponse.from(details)).hasStatusOk().hasHeader("ETag", "\"2\"");
		assertThat(RestTestClientResponse.from(details)).bodyJson().isLenientlyEqualTo("""
				{
				  "status": "APPROVED",
				  "statusHistory": [
				    { "fromStatus": "RECEIVED",  "toStatus": "IN_REVIEW", "reason": "Tildelt saksbehandler" },
				    { "fromStatus": "IN_REVIEW", "toStatus": "APPROVED",  "reason": "Oppfyller kriteriene" }
				  ],
				  "notes": [
				    { "text": "Ringte søker, budsjett kommer på e-post." }
				  ]
				}
				""");

		// And it counts in the statistics
		var statistics = client.get().uri("/api/statistics").exchange();
		assertThat(RestTestClientResponse.from(statistics)).bodyJson().isLenientlyEqualTo("""
				{
				  "byStatus": [
				    { "status": "RECEIVED",  "applicationCount": 0, "totalAmount": 0 },
				    { "status": "IN_REVIEW", "applicationCount": 0, "totalAmount": 0 },
				    { "status": "APPROVED",  "applicationCount": 1, "totalAmount": 25000 },
				    { "status": "REJECTED",  "applicationCount": 0, "totalAmount": 0 }
				  ],
				  "applicationCount": 1,
				  "totalAmount": 25000
				}
				""");
	}

	@Test
	void caseOfficerWorkingOnAnOutdatedVersionCannotOverwriteAColleague() {
		// given two case officers who both saw version 1
		var id = createApplication();
		changeStatus(id, "\"0\"", "IN_REVIEW", "Tildelt saksbehandler");
		changeStatus(id, "\"1\"", "APPROVED", "Oppfyller kriteriene");

		// when the second one tries to reject based on what they saw
		var rejected = changeStatus(id, "\"1\"", "REJECTED", "Mangler budsjett");

		// then
		assertThat(RestTestClientResponse.from(rejected)).hasStatus(HttpStatus.PRECONDITION_FAILED);
		assertThat(RestTestClientResponse.from(client.get().uri("/api/grant-applications/{id}", id).exchange()))
				.bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
	}

	@Test
	void statusChangeWithoutIfMatchIsRejected() {
		var id = createApplication();

		var response = client.post().uri("/api/grant-applications/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("status", "IN_REVIEW", "reason", "Tildelt saksbehandler"))
				.exchange();

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.PRECONDITION_REQUIRED)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void skippingReviewIsAnInvalidTransition() {
		var id = createApplication();

		var response = changeStatus(id, "\"0\"", "APPROVED", "Hopper over behandling");

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.CONFLICT);
		assertThat(RestTestClientResponse.from(response)).bodyJson()
				.extractingPath("$.allowedStatuses").asArray().containsExactly("IN_REVIEW");
	}

	@Test
	void unknownOrganizationCannotApply() {
		var response = submit(applicationFor(UNKNOWN_ORGANIZATION));

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(repository.count()).isZero();
	}

	@Test
	void deletedOrganizationCannotApply() {
		var response = submit(applicationFor(DELETED_ORGANIZATION));

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(RestTestClientResponse.from(response)).bodyJson().extractingPath("$.detail")
				.isEqualTo("Organization 509100675 was deleted on 2024-03-09");
	}

	@Test
	void applicationIsNotSavedWhenTheRegisterIsDown() {
		var response = submit(applicationFor(ORGANIZATION_WHEN_REGISTER_FAILS));

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(repository.count()).isZero();
	}

	@Test
	void invalidApplicationIsRejectedWithoutCallingTheRegister() {
		var response = submit(Map.of(
				"organizationNumber", "974760674",
				"applicantName", "Ola Nordmann",
				"applicantEmail", "ola.nordmann@example.com",
				"amount", 25_000,
				"purpose", "Nye drakter til barnelaget"));

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(brreg.getAllServeEvents()).isEmpty();
	}

	@Test
	void unknownApplicationIsNotFound() {
		var response = client.get().uri("/api/grant-applications/{id}", UUID.randomUUID()).exchange();

		assertThat(RestTestClientResponse.from(response)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private UUID createApplication() {
		return submit(applicationFor(ACTIVE_ORGANIZATION))
				.returnResult(GrantApplicationDetails.class)
				.getResponseBody()
				.id();
	}

	private RestTestClient.ResponseSpec submit(Map<String, Object> application) {
		return client.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.body(application)
				.exchange();
	}

	private RestTestClient.ResponseSpec changeStatus(UUID id, String ifMatch, String status, String reason) {
		return client.post().uri("/api/grant-applications/{id}/status", id)
				.header("If-Match", ifMatch)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("status", status, "reason", reason))
				.exchange();
	}

	private static Map<String, Object> applicationFor(String organizationNumber) {
		return Map.of(
				"organizationNumber", organizationNumber,
				"applicantName", "Ola Nordmann",
				"applicantEmail", "ola.nordmann@example.com",
				"amount", 25_000,
				"purpose", "Nye drakter til barnelaget");
	}

}
