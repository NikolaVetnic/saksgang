package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.util.UUID;

/**
 * The client based its change on a version of the application that is no longer the current one.
 */
public class GrantApplicationModifiedException extends RuntimeException {

	public GrantApplicationModifiedException(UUID id, long expectedVersion, long currentVersion) {
		super("Grant application %s has been modified (expected version %d, current version %d)"
				.formatted(id, expectedVersion, currentVersion));
	}

}
