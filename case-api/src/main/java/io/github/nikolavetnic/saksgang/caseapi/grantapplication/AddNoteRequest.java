package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(@NotBlank @Size(max = 2000) String text) {
}
