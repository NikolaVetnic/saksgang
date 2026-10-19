package io.github.nikolavetnic.saksgang.caseapi.organization;

import java.time.LocalDate;

/**
 * An organization as registered in the Central Coordinating Register for Legal Entities.
 *
 * @param deletedOn the date the organization was deleted from the register, or {@code null} if it is active
 */
public record Organization(String organizationNumber, String name, LocalDate deletedOn) {

	public boolean isDeleted() {
		return deletedOn != null;
	}

}
