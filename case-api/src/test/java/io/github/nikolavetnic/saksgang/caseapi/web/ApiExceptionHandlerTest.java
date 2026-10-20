package io.github.nikolavetnic.saksgang.caseapi.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplication;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationNotFoundException;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.InvalidStatusTransitionException;
import io.github.nikolavetnic.saksgang.caseapi.organization.DeletedOrganizationException;
import io.github.nikolavetnic.saksgang.caseapi.organization.Organization;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationNumber;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistryUnavailableException;
import io.github.nikolavetnic.saksgang.caseapi.organization.UnknownOrganizationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests the error mapping on its own, through a controller that only exists in this test. The real
 * controllers test their own behaviour; this keeps the two independent as new endpoints are added.
 */
@WebMvcTest(controllers = ApiExceptionHandlerTest.ThrowingController.class)
@Import(ApiExceptionHandlerTest.ThrowingController.class)
class ApiExceptionHandlerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void invalidRequestGivesFieldErrorsInEnglishWhateverTheClientLanguage() {
		// when
		var result = mvc.post().uri("/test/validated")
				.header(HttpHeaders.ACCEPT_LANGUAGE, "de")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "organizationNumber": "974760674", "email": "not-an-email" }
						""")
				.exchange();

		// then
		assertProblem(result, HttpStatus.BAD_REQUEST, "Invalid request");
		assertThat(result).bodyJson().extractingPath("$.errors.organizationNumber")
				.asArray().containsExactly("must be a valid organization number");
		assertThat(result).bodyJson().extractingPath("$.errors.email")
				.asArray().containsExactly("must be a well-formed email address");
	}

	@Test
	void malformedJsonGivesBadRequest() {
		var result = mvc.post().uri("/test/validated")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ not json")
				.exchange();

		assertProblem(result, HttpStatus.BAD_REQUEST, "Bad Request");
	}

	@Test
	void unknownEnumValueInQueryGivesBadRequest() {
		var result = mvc.get().uri("/test/status?status=MOTTATT").exchange();

		assertProblem(result, HttpStatus.BAD_REQUEST, "Bad Request");
	}

	@Test
	void unknownSortPropertyGivesBadRequest() {
		var result = mvc.get().uri("/test/throw/invalid-sort").exchange();

		assertProblem(result, HttpStatus.BAD_REQUEST, "Invalid sort property");
	}

	@Test
	void unknownApplicationGivesNotFound() {
		var result = mvc.get().uri("/test/throw/not-found").exchange();

		assertProblem(result, HttpStatus.NOT_FOUND, "Grant application not found");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Grant application 0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77 does not exist");
	}

	@Test
	void invalidStatusTransitionGivesConflictWithCurrentAndAllowedStatuses() {
		var result = mvc.get().uri("/test/throw/invalid-transition").exchange();

		assertProblem(result, HttpStatus.CONFLICT, "Invalid status transition");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Cannot change status from RECEIVED to APPROVED");
		assertThat(result).bodyJson().extractingPath("$.currentStatus").isEqualTo("RECEIVED");
		assertThat(result).bodyJson().extractingPath("$.allowedStatuses").asArray().containsExactly("IN_REVIEW");
	}

	@Test
	void unknownOrganizationGivesUnprocessableContent() {
		var result = mvc.get().uri("/test/throw/unknown-organization").exchange();

		assertProblem(result, HttpStatus.UNPROCESSABLE_CONTENT, "Organization cannot apply");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Organization 974760673 is not registered");
	}

	@Test
	void deletedOrganizationGivesUnprocessableContent() {
		var result = mvc.get().uri("/test/throw/deleted-organization").exchange();

		assertProblem(result, HttpStatus.UNPROCESSABLE_CONTENT, "Organization cannot apply");
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Organization 974760673 was deleted on 2024-03-09");
	}

	@Test
	void unavailableRegistryGivesServiceUnavailableWithoutInternalDetails() {
		var result = mvc.get().uri("/test/throw/registry-unavailable").exchange();

		assertProblem(result, HttpStatus.SERVICE_UNAVAILABLE, "Organization registry unavailable");
		assertThat(result).bodyText().doesNotContain("brreg", "timed out");
	}

	@Test
	void unexpectedErrorGivesInternalServerErrorWithoutInternalDetails() {
		var result = mvc.get().uri("/test/throw/unexpected").exchange();

		assertProblem(result, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
		assertThat(result).bodyText().doesNotContain("connection pool");
	}

	@Test
	void unknownRouteGivesNotFound() {
		var result = mvc.get().uri("/test/does-not-exist").exchange();

		assertProblem(result, HttpStatus.NOT_FOUND, "Not Found");
	}

	private static void assertProblem(MvcTestResult result, HttpStatus status, String title) {
		assertThat(result).hasStatus(status)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(status.value());
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo(title);
	}

	record ValidatedRequest(@OrganizationNumber String organizationNumber, @Email String email) {
	}

	@RestController
	static class ThrowingController {

		@PostMapping("/test/validated")
		void validated(@Valid @RequestBody ValidatedRequest request) {
		}

		@GetMapping("/test/status")
		void status(@RequestParam GrantApplicationStatus status) {
		}

		@GetMapping("/test/throw/invalid-sort")
		void invalidSort() {
			throw new PropertyReferenceException("secret", TypeInformation.of(GrantApplication.class), List.of());
		}

		@GetMapping("/test/throw/not-found")
		void notFound() {
			throw new GrantApplicationNotFoundException(UUID.fromString("0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77"));
		}

		@GetMapping("/test/throw/invalid-transition")
		void invalidTransition() {
			throw new InvalidStatusTransitionException(GrantApplicationStatus.RECEIVED,
					GrantApplicationStatus.APPROVED);
		}

		@GetMapping("/test/throw/unknown-organization")
		void unknownOrganization() {
			throw new UnknownOrganizationException("974760673");
		}

		@GetMapping("/test/throw/deleted-organization")
		void deletedOrganization() {
			throw new DeletedOrganizationException(
					new Organization("974760673", "Nedlagt Forening", LocalDate.of(2024, 3, 9)));
		}

		@GetMapping("/test/throw/registry-unavailable")
		void registryUnavailable() {
			throw new OrganizationRegistryUnavailableException("974760673",
					new IllegalStateException("connect timed out to data.brreg.no"));
		}

		@GetMapping("/test/throw/unexpected")
		void unexpected() {
			throw new IllegalStateException("connection pool exhausted");
		}

	}

}
