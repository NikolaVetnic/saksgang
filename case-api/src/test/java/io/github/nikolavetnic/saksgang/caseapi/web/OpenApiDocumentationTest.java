package io.github.nikolavetnic.saksgang.caseapi.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nikolavetnic.saksgang.caseapi.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void apiDocumentationDescribesTheGrantApplicationEndpoints() {
		var result = mvc.get().uri("/v3/api-docs").exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.info.title").isEqualTo("Saksgang case API");
		assertThat(result).bodyJson().extractingPath("$.paths").asMap()
				.containsKeys("/api/grant-applications", "/api/grant-applications/{id}");
	}

	@Test
	void swaggerUiIsAvailable() {
		var result = mvc.get().uri("/swagger-ui.html").exchange();

		assertThat(result).hasStatus(HttpStatus.FOUND);
	}

}
