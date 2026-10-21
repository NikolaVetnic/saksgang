package io.github.nikolavetnic.saksgang.caseapi.web;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationModifiedException;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationNotFoundException;
import io.github.nikolavetnic.saksgang.caseapi.grantapplication.InvalidStatusTransitionException;
import io.github.nikolavetnic.saksgang.caseapi.organization.DeletedOrganizationException;
import io.github.nikolavetnic.saksgang.caseapi.organization.OrganizationRegistryUnavailableException;
import io.github.nikolavetnic.saksgang.caseapi.organization.UnknownOrganizationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 Problem Details. Spring MVC's own exceptions (malformed JSON, unknown
 * parameters, missing routes) are handled by the base class.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler
	ProblemDetail handleNotFound(GrantApplicationNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Grant application not found", exception.getMessage());
	}

	@ExceptionHandler({ UnknownOrganizationException.class, DeletedOrganizationException.class })
	ProblemDetail handleIneligibleOrganization(RuntimeException exception) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Organization cannot apply", exception.getMessage());
	}

	@ExceptionHandler
	ProblemDetail handleInvalidTransition(InvalidStatusTransitionException exception) {
		var problem = problem(HttpStatus.CONFLICT, "Invalid status transition", exception.getMessage());
		problem.setProperty("currentStatus", exception.getFrom());
		problem.setProperty("allowedStatuses", exception.getFrom().allowedNextStatuses());
		return problem;
	}

	@ExceptionHandler
	ProblemDetail handleModified(GrantApplicationModifiedException exception) {
		return problem(HttpStatus.PRECONDITION_FAILED, "Grant application has been modified", exception.getMessage());
	}

	/**
	 * Two requests passed the version check at the same time; the database update caught the second one.
	 */
	@ExceptionHandler
	ProblemDetail handleConcurrentModification(OptimisticLockingFailureException exception) {
		return problem(HttpStatus.PRECONDITION_FAILED, "Grant application has been modified",
				"The grant application was changed by someone else. Fetch it again and retry.");
	}

	@ExceptionHandler
	ProblemDetail handleRegistryUnavailable(OrganizationRegistryUnavailableException exception) {
		log.warn("Organization registry unavailable", exception);
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Organization registry unavailable",
				"Could not verify the organization right now. Please try again later.");
	}

	@ExceptionHandler
	ProblemDetail handleInvalidSortProperty(PropertyReferenceException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid sort property", exception.getMessage());
	}

	@ExceptionHandler
	ProblemDetail handleUnexpected(Exception exception) {
		log.error("Unexpected error", exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
				"Something went wrong on our side.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "One or more fields are invalid.");
		problem.setProperty("errors", fieldErrors(exception));
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	private static Map<String, List<String>> fieldErrors(MethodArgumentNotValidException exception) {
		return exception.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.groupingBy(FieldError::getField,
						Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
