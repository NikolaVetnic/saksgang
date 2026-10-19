package io.github.nikolavetnic.saksgang.caseapi.organization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * The parts of an "enhet" from the Brønnøysund Register Centre that we use. Covers both active and deleted
 * entities; deleted ones have {@code slettedato} set.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record BrregEntity(
		@JsonProperty("organisasjonsnummer") String organizationNumber,
		@JsonProperty("navn") String name,
		@JsonProperty("slettedato") LocalDate deletedOn) {

	Organization toOrganization() {
		return new Organization(organizationNumber, name, deletedOn);
	}

}
