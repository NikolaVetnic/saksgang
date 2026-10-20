package io.github.nikolavetnic.saksgang.caseapi.statistics;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;

public record StatusCount(GrantApplicationStatus status, long applicationCount, long totalAmount) {
}
