package io.github.nikolavetnic.saksgang.sak.organization;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrganizationNumberValidator implements ConstraintValidator<OrganizationNumber, String> {

	private static final int[] WEIGHTS = { 3, 2, 7, 6, 5, 4, 3, 2 };

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || isValid(value);
	}

	/**
	 * Checks length, digits only and the mod11 check digit used by the Brønnøysund Register Centre.
	 */
	public static boolean isValid(String organizationNumber) {
		if (!organizationNumber.matches("\\d{9}")) {
			return false;
		}

		int sum = 0;
		for (int i = 0; i < WEIGHTS.length; i++) {
			sum += Character.digit(organizationNumber.charAt(i), 10) * WEIGHTS[i];
		}

		int remainder = sum % 11;
		int expectedCheckDigit = remainder == 0 ? 0 : 11 - remainder;
		if (expectedCheckDigit == 10) {
			return false;
		}
		return Character.digit(organizationNumber.charAt(8), 10) == expectedCheckDigit;
	}

}
