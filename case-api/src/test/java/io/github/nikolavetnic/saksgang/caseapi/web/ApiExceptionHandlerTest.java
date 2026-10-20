package io.github.nikolavetnic.saksgang.caseapi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplication;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationNotFoundException;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationService;
import io.github.nikolavetnic.saksgang.caseapi.organization.DeletedOrganizationException;
import io.github.nikolavetnic.saksgang.caseapi.organization.Organization;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistryUnavailableException;
import io.github.nikolavetnic.saksgang.caseapi.organization.UnknownOrganizationException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest
class ApiExceptionHandlerTest {

	private static final String VALID_REQUEST = """
			{
			  "organizationNumber": "974760673",
			  "applicantName": "Ola Nordmann",
			  "applicantEmail": "ola.nordmann@example.com",
			  "amount": 25000,
			  "purpose": "Nye drakter til barnelaget"
			}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GrantApplicationService service;

	@Test
	void invalidRequestGivesFieldErrorsInEnglishWhateverTheClientLanguage() {
		// when
		var result = mvc.post().uri("/api/grant-applications")
				.header(HttpHeaders.ACCEPT_LANGUAGE, "de")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "organizationNumber": "974760674",
						  "applicantName": "Ola Nordmann",
						  "applicantEmail": "not-an-email",
						  "amount": 25000,
						  "purpose": "Nye drakter til barnelaget"
						}
						""")
				.exchange();

		// then
		assertProblem(result, HttpStatus.BAD_REQUEST, "Invalid request");
		assertThat(result).bodyJson().extractingPath("$.errors.organizationNumber")
				.asArray().containsExactly("must be a valid organization number");
		assertThat(result).bodyJson().extractingPath("$.errors.applicantEmail")
				.asArray().containsExactly("must be a well-formed email address");
	}

	@Test
	void malformedJsonGivesBadRequest() {
		var result = mvc.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ not json")
				.exchange();

		assertProblem(result, HttpStatus.BAD_REQUEST, "Bad Request");
	}

	@Test
	void unknownStatusFilterGivesBadRequest() {
		var result = mvc.get().uri("/api/grant-applications?status=MOTTATT").exchange();

		assertProblem(result, HttpStatus.BAD_REQUEST, "Bad Request");
	}

	@Test
	void unknownSortPropertyGivesBadRequest() {
		// given
		when(service.list(any(), any())).thenThrow(
				new PropertyReferenceException("secret", TypeInformation.of(GrantApplication.class), List.of()));

		// when
		var result = mvc.get().uri("/api/grant-applications?sort=secret").exchange();

		// then
		assertProblem(result, HttpStatus.BAD_REQUEST, "Invalid sort property");
	}

	@Test
	void unknownApplicationGivesNotFound() {
		// given
		var id = UUID.fromString("0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77");
		when(service.get(id)).thenThrow(new GrantApplicationNotFoundException(id));

		// when
		var result = mvc.get().uri("/api/grant-applications/{id}", id).exchange();

		// then
		assertProblem(result, HttpStatus.NOT_FOUND, "Grant application not found");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Grant application 0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77 does not exist");
	}

	@Test
	void unknownOrganizationGivesUnprocessableContent() {
		when(service.create(any())).thenThrow(new UnknownOrganizationException("974760673"));

		var result = postValidRequest();

		assertProblem(result, HttpStatus.UNPROCESSABLE_CONTENT, "Organization cannot apply");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Organization 974760673 is not registered");
	}

	@Test
	void deletedOrganizationGivesUnprocessableContent() {
		when(service.create(any())).thenThrow(new DeletedOrganizationException(
				new Organization("974760673", "Nedlagt Forening", LocalDate.of(2024, 3, 9))));

		var result = postValidRequest();

		assertProblem(result, HttpStatus.UNPROCESSABLE_CONTENT, "Organization cannot apply");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Organization 974760673 was deleted on 2024-03-09");
	}

	@Test
	void unavailableRegistryGivesServiceUnavailableWithoutInternalDetails() {
		when(service.create(any())).thenThrow(new OrganizationRegistryUnavailableException("974760673",
				new IllegalStateException("connect timed out to data.brreg.no")));

		var result = postValidRequest();

		assertProblem(result, HttpStatus.SERVICE_UNAVAILABLE, "Organization registry unavailable");
		assertThat(result).bodyText().doesNotContain("brreg", "timed out");
	}

	@Test
	void unexpectedErrorGivesInternalServerErrorWithoutInternalDetails() {
		var id = UUID.randomUUID();
		when(service.get(id)).thenThrow(new IllegalStateException("connection pool exhausted"));

		var result = mvc.get().uri("/api/grant-applications/{id}", id).exchange();

		assertProblem(result, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
		assertThat(result).bodyText().doesNotContain("connection pool");
	}

	@Test
	void unknownRouteGivesNotFound() {
		var result = mvc.get().uri("/api/does-not-exist").exchange();

		assertProblem(result, HttpStatus.NOT_FOUND, "Not Found");
	}

	private MvcTestResult postValidRequest() {
		return mvc.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_REQUEST)
				.exchange();
	}

	private static void assertProblem(MvcTestResult result, HttpStatus status, String title) {
		assertThat(result).hasStatus(status)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(status.value());
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo(title);
	}

}
