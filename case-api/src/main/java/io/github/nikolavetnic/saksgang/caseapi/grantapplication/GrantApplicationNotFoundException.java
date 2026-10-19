package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.util.UUID;

public class GrantApplicationNotFoundException extends RuntimeException {

	public GrantApplicationNotFoundException(UUID id) {
		super("Grant application " + id + " does not exist");
	}

}
