package io.github.nikolavetnic.saksgang.caseapi.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.nikolavetnic.saksgang.caseapi.grantapplication.GrantApplicationStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private StatisticsService service;

	@Test
	void returnsCountsPerStatusAndTotals() {
		// given
		when(service.grantApplicationStatistics()).thenReturn(new GrantApplicationStatistics(
				List.of(new StatusCount(GrantApplicationStatus.RECEIVED, 2, 25_000)), 2, 25_000));

		// when
		var result = mvc.get().uri("/api/statistics").exchange();

		// then
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().isLenientlyEqualTo("""
				{
				  "byStatus": [
				    { "status": "RECEIVED", "applicationCount": 2, "totalAmount": 25000 }
				  ],
				  "applicationCount": 2,
				  "totalAmount": 25000
				}
				""");
	}

}
