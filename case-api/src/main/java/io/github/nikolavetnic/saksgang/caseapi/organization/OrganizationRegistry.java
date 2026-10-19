package io.github.nikolavetnic.saksgang.caseapi.organization;

import java.util.Optional;

public interface OrganizationRegistry {

	/**
	 * Looks up an organization, including deleted ones.
	 *
	 * @return the organization, or empty if the number is unknown to the register
	 * @throws OrganizationRegistryUnavailableException if the register cannot be reached or fails
	 */
	Optional<Organization> findByOrganizationNumber(String organizationNumber);

}
