package io.github.nikolavetnic.saksgang.caseapi.organization;

public class DeletedOrganizationException extends RuntimeException {

	public DeletedOrganizationException(Organization organization) {
		super("Organization %s was deleted on %s".formatted(organization.organizationNumber(),
				organization.deletedOn()));
	}

}
