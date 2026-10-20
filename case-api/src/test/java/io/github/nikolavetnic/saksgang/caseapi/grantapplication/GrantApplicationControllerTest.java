package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GrantApplicationController.class)
class GrantApplicationControllerTest {

	private static final UUID ID = UUID.fromString("0199f3a2-5c1e-7b3d-9a4f-2e8c6d1b0a77");
	private static final Instant CREATED_AT = Instant.parse("2026-10-19T08:30:00Z");

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GrantApplicationService service;

	@Test
	void createReturns201WithLocationAndCreatedApplication() {
		// given
		when(service.create(any())).thenReturn(details(GrantApplicationStatus.RECEIVED));

		// when
		var result = mvc.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "organizationNumber": "974760673",
						  "applicantName": "Ola Nordmann",
						  "applicantEmail": "ola.nordmann@example.com",
						  "amount": 25000,
						  "purpose": "Nye drakter til barnelaget"
						}
						""")
				.exchange();

		// then
		assertThat(result).hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/grant-applications/" + ID);
		assertThat(result).bodyJson().extractingPath("$.organizationName").isEqualTo("REGISTERENHETEN I BRØNNØYSUND");
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("RECEIVED");
	}

	@Test
	void createRejectsInvalidRequestWithoutCallingService() {
		// when
		var result = mvc.post().uri("/api/grant-applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "organizationNumber": "974760674",
						  "applicantName": "",
						  "applicantEmail": "not-an-email",
						  "amount": 0,
						  "purpose": "Nye drakter til barnelaget"
						}
						""")
				.exchange();

		// then
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(service);
	}

	@Test
	void listPassesStatusFilterAndPagingToService() {
		// given
		var summary = new GrantApplicationSummary(ID, "974760673", "REGISTERENHETEN I BRØNNØYSUND", "Ola Nordmann",
				25_000, GrantApplicationStatus.IN_REVIEW, CREATED_AT);
		when(service.list(eq(GrantApplicationStatus.IN_REVIEW), any()))
				.thenAnswer(invocation -> new PageImpl<>(List.of(summary), invocation.getArgument(1), 11));

		// when
		var result = mvc.get().uri("/api/grant-applications?status=IN_REVIEW&page=2&size=5").exchange();

		// then
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.content[0].id").isEqualTo(ID.toString());
		assertThat(result).bodyJson().extractingPath("$.page.number").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(11);

		var pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).list(eq(GrantApplicationStatus.IN_REVIEW), pageable.capture());
		assertThat(pageable.getValue()).isEqualTo(
				PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
	}

	@Test
	void listRejectsUnknownStatus() {
		var result = mvc.get().uri("/api/grant-applications?status=MOTTATT").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(service);
	}

	@Test
	void getReturnsDetailsWithStatusHistory() {
		// given
		when(service.get(ID)).thenReturn(details(GrantApplicationStatus.IN_REVIEW));

		// when
		var result = mvc.get().uri("/api/grant-applications/{id}", ID).exchange();

		// then
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.statusHistory[0].toStatus").isEqualTo("IN_REVIEW");
		assertThat(result).bodyJson().extractingPath("$.statusHistory[0].reason").isEqualTo("Tildelt saksbehandler");
	}

	@Test
	void changeStatusReturnsUpdatedApplication() {
		// given
		when(service.changeStatus(eq(ID), any())).thenReturn(details(GrantApplicationStatus.IN_REVIEW));

		// when
		var result = mvc.post().uri("/api/grant-applications/{id}/status", ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "status": "IN_REVIEW", "reason": "Tildelt saksbehandler" }
						""")
				.exchange();

		// then
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("IN_REVIEW");
		verify(service).changeStatus(ID, new ChangeStatusRequest(GrantApplicationStatus.IN_REVIEW,
				"Tildelt saksbehandler"));
	}

	@Test
	void changeStatusRequiresReason() {
		var result = mvc.post().uri("/api/grant-applications/{id}/status", ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "status": "IN_REVIEW" }
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.reason").asArray().isNotEmpty();
		verifyNoInteractions(service);
	}

	@Test
	void addNoteReturns201WithTheNote() {
		// given
		var noteId = UUID.fromString("0199f3a2-6d2f-7c4e-8b5a-3f9d7e2c1b88");
		when(service.addNote(ID, new AddNoteRequest("Ringte søker, budsjett kommer.")))
				.thenReturn(new GrantApplicationDetails.NoteEntry(noteId, "Ringte søker, budsjett kommer.", CREATED_AT));

		// when
		var result = mvc.post().uri("/api/grant-applications/{id}/notes", ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "text": "Ringte søker, budsjett kommer." }
						""")
				.exchange();

		// then
		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(noteId.toString());
		assertThat(result).bodyJson().extractingPath("$.text").isEqualTo("Ringte søker, budsjett kommer.");
	}

	@Test
	void addNoteRejectsBlankText() {
		var result = mvc.post().uri("/api/grant-applications/{id}/notes", ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "text": "  " }
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(service);
	}

	private static GrantApplicationDetails details(GrantApplicationStatus status) {
		var history = status == GrantApplicationStatus.RECEIVED
				? List.<GrantApplicationDetails.StatusChangeEntry>of()
				: List.of(new GrantApplicationDetails.StatusChangeEntry(GrantApplicationStatus.RECEIVED, status,
						"Tildelt saksbehandler", CREATED_AT));
		return new GrantApplicationDetails(ID, "974760673", "REGISTERENHETEN I BRØNNØYSUND", "Ola Nordmann",
				"ola.nordmann@example.com", 25_000, "Nye drakter til barnelaget", status, CREATED_AT, CREATED_AT,
				history, List.of());
	}

}
