package io.github.nikolavetnic.saksgang.caseapi.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrganizationNumberValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"974760673", // Brønnøysundregistrene
			"123456785",
	})
	void acceptsNumberWithCorrectCheckDigit(String organizationNumber) {
		assertThat(OrganizationNumberValidator.isValid(organizationNumber)).isTrue();
	}

	@Test
	void acceptsCheckDigitZeroWhenRemainderIsZero() {
		// 1*2 + 1*7 + 1*2 = 11, remainder 0 gives check digit 0
		assertThat(OrganizationNumberValidator.isValid("011000010")).isTrue();
	}

	@Test
	void rejectsWrongCheckDigit() {
		assertThat(OrganizationNumberValidator.isValid("974760674")).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "000000060", "000000061", "000000065", "000000069" })
	void rejectsAnyCheckDigitWhenRemainderIsOne(String organizationNumber) {
		// 6*2 = 12, remainder 1 gives check digit 10, which cannot be a single digit
		assertThat(OrganizationNumberValidator.isValid(organizationNumber)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "97476067", "9747606730", "97476067a", "974 760 673", "-97476067" })
	void rejectsAnythingThatIsNotExactlyNineDigits(String organizationNumber) {
		assertThat(OrganizationNumberValidator.isValid(organizationNumber)).isFalse();
	}

}
