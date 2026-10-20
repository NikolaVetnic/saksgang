package io.github.nikolavetnic.saksgang.caseapi.statistics;

import java.util.List;

public record GrantApplicationStatistics(List<StatusCount> byStatus, long applicationCount, long totalAmount) {
}
