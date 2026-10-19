package io.github.nikolavetnic.saksgang.caseapi.organization;

public class UnknownOrganizationException extends RuntimeException {

	public UnknownOrganizationException(String organizationNumber) {
		super("Organization " + organizationNumber + " is not registered");
	}

}
