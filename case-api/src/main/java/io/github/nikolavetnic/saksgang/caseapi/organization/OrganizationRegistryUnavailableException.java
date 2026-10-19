package io.github.nikolavetnic.saksgang.caseapi.organization;

public class OrganizationRegistryUnavailableException extends RuntimeException {

	public OrganizationRegistryUnavailableException(String organizationNumber, Throwable cause) {
		super("Could not look up organization " + organizationNumber + " in the register", cause);
	}

}
