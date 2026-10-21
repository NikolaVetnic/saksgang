package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/grant-applications")
class GrantApplicationController {

	private final GrantApplicationService service;

	GrantApplicationController(GrantApplicationService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<GrantApplicationDetails> create(@Valid @RequestBody CreateGrantApplicationRequest request) {
		var created = service.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).eTag(String.valueOf(created.version())).body(created);
	}

	@GetMapping
	Page<GrantApplicationSummary> list(
			@RequestParam(required = false) GrantApplicationStatus status,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.list(status, pageable);
	}

	@GetMapping("/{id}")
	ResponseEntity<GrantApplicationDetails> get(@PathVariable UUID id) {
		return withETag(service.get(id));
	}

	/**
	 * Requires the ETag from a previous read in If-Match, so a case officer cannot overwrite a change they
	 * have not seen.
	 */
	@PostMapping("/{id}/status")
	ResponseEntity<GrantApplicationDetails> changeStatus(@PathVariable UUID id,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
			@Valid @RequestBody ChangeStatusRequest request) {
		return withETag(service.changeStatus(id, versionFrom(ifMatch), request));
	}

	@PostMapping("/{id}/notes")
	@ResponseStatus(HttpStatus.CREATED)
	GrantApplicationDetails.NoteEntry addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
		return service.addNote(id, request);
	}

	private static ResponseEntity<GrantApplicationDetails> withETag(GrantApplicationDetails details) {
		return ResponseEntity.ok().eTag(String.valueOf(details.version())).body(details);
	}

	private static long versionFrom(String ifMatch) {
		if (ifMatch == null) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
					"If-Match header with the ETag of the grant application is required");
		}
		var tag = ifMatch.strip();
		if (tag.length() > 2 && tag.startsWith("\"") && tag.endsWith("\"")) {
			try {
				return Long.parseLong(tag.substring(1, tag.length() - 1));
			}
			catch (NumberFormatException ignored) {
				// falls through to the precondition failure below
			}
		}
		throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
				"If-Match does not match the current ETag of the grant application");
	}

}
