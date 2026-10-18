package io.github.nikolavetnic.saksgang.caseapi.organization;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrganizationNumberConstraintTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	record Request(@OrganizationNumber String organizationNumber) {
	}

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void validNumberGivesNoViolations() {
		assertThat(validator.validate(new Request("974760673"))).isEmpty();
	}

	@Test
	void invalidNumberGivesViolationOnTheAnnotatedField() {
		// when
		var violations = validator.validate(new Request("974760674"));

		// then
		assertThat(violations).singleElement().satisfies(violation -> {
			assertThat(violation.getPropertyPath()).hasToString("organizationNumber");
			assertThat(violation.getMessage()).isEqualTo("must be a valid organization number");
		});
	}

	@Test
	void nullIsLeftToNotNull() {
		assertThat(validator.validate(new Request(null))).isEmpty();
	}

}
