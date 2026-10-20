package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	Page<GrantApplicationSummary> list(
			@RequestParam(required = false) GrantApplicationStatus status,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.list(status, pageable);
	}

	@GetMapping("/{id}")
	GrantApplicationDetails get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PostMapping("/{id}/status")
	GrantApplicationDetails changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
		return service.changeStatus(id, request);
	}

	@PostMapping("/{id}/notes")
	@ResponseStatus(HttpStatus.CREATED)
	GrantApplicationDetails.NoteEntry addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
		return service.addNote(id, request);
	}

}
