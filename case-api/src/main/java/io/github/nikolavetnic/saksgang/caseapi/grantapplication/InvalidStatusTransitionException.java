package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

public class InvalidStatusTransitionException extends RuntimeException {

	private final GrantApplicationStatus from;
	private final GrantApplicationStatus to;

	public InvalidStatusTransitionException(GrantApplicationStatus from, GrantApplicationStatus to) {
		super("Cannot change status from %s to %s".formatted(from, to));
		this.from = from;
		this.to = to;
	}

	public GrantApplicationStatus getFrom() {
		return from;
	}

	public GrantApplicationStatus getTo() {
		return to;
	}

}
