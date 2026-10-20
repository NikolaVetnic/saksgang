package io.github.nikolavetnic.saksgang.caseapi.statistics;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reporting queries written in plain SQL. Aggregations like these are clearer in SQL than in JPQL, and they
 * do not need entities to be loaded.
 */
@Repository
class StatisticsRepository {

	private final JdbcClient jdbcClient;

	StatisticsRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	List<StatusCount> countByStatus() {
		return jdbcClient.sql("""
				select status,
				       count(*)    as application_count,
				       sum(amount) as total_amount
				from grant_application
				group by status
				""")
				.query((row, rowNumber) -> new StatusCount(
						GrantApplicationStatus.valueOf(row.getString("status")),
						row.getLong("application_count"),
						row.getLong("total_amount")))
				.list();
	}

}
